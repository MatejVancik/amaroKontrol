package com.mv2studio.amarok.kontrol.ui.Fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.communication.ConnectionMessage;
import com.mv2studio.amarok.kontrol.communication.ConnectorUpdateCallback;
import com.mv2studio.amarok.kontrol.helpers.BitmapConcurrencyHandler;
import com.mv2studio.amarok.kontrol.helpers.CommonHelper;
import com.mv2studio.amarok.kontrol.shared.PlayingState;
import com.mv2studio.amarok.kontrol.shared.model.Artist;
import com.mv2studio.amarok.kontrol.shared.model.Song;
import com.mv2studio.amarok.kontrol.tasks.GetCollectionPhotosTask;
import com.mv2studio.amarok.kontrol.ui.Activity.BaseActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class CollectionFragment extends BaseFragment implements OnItemClickListener {

	private AbsListView mListView;
    private ViewGroup mViewContainer;
	private RelativeLayout mEmptyView;
	private TextView mEmptyViewText;
	private ArtistAdapter mListAdapter;
    private boolean isListLoaded;
    private PlayingState mLastKnownPlayingState = PlayingState.PLAYING;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.mViewContainer = container;
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_collection, container, false);

		mListView = (AbsListView) layout.findViewById(R.id.artists_list);
		mEmptyView = (RelativeLayout) layout.findViewById(R.id.fragment_artists_empty_view);
		mEmptyViewText = (TextView) layout.findViewById(R.id.fragment_artists_empty_text);
		mListView.setEmptyView(mEmptyView);
        mListView.setOnItemClickListener(this);

		if (Build.VERSION.SDK_INT >= 19) {
            // fast scroll is buggy somehow on kitkat
			mListView.setFastScrollAlwaysVisible(true);
		}

		new FillArtistsAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

		return layout;
	}

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout lay = (LinearLayout) inflater.inflate(R.layout.fragment_collection, mViewContainer, false);
		ListView alw = (ListView) lay.findViewById(R.id.artists_list);
		ViewGroup root = (ViewGroup) getView();

        root.removeAllViews();
		lay.removeAllViews();
		root.addView(alw);
		alw.setAdapter(mListAdapter);
		alw.setSelection(mListView.getFirstVisiblePosition());
		alw.setOnItemClickListener(this);
		mListView = alw;
		super.onConfigurationChanged(newConfig);
	}

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && !isListLoaded) {
            // refresh if was not done yet
            new FillArtistsAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void)null);
        }
    }

    @Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Bundle bundle = new Bundle();
		bundle.putInt(ArtistFragment.ARG_ARTIST_ID, ((Artist) mListView.getItemAtPosition(arg2)).getId());

        ((BaseActivity) getActivity()).pushFragment(R.id.fragArtistDetail, bundle);
	}

    private ConnectorUpdateCallback mPlaylistRefreshCallback = new ConnectorUpdateCallback() {
        @Override
        public void onDataUpdated(Song song, PlayingState state) { }

        @Override
        public void onDataUnavailable(int errorCode, ConnectionMessage connectionMessage) { }

        @Override
        public void onPlayingStateChanged(PlayingState state) {
            if (mLastKnownPlayingState == PlayingState.DOWN && state != PlayingState.DOWN) {
                // refresh each time player is connected
                new FillArtistsAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
            }
            mLastKnownPlayingState = state;
        }
    };

    /**
	 * ***************************************************************************************
	 * ***********************************  A D A P T E R  ***********************************
	 * ***************************************************************************************
	 */
	
	public class ArtistAdapter extends ArrayAdapter<Artist> implements SectionIndexer{

		private ArrayList<Artist> mData;
		private Context mContext;
		private HashMap<String, Integer> mAlphaIndexer;
	    private String[] mSections;
	    private BitmapConcurrencyHandler mBch;

		public ArtistAdapter(final Context context, int textViewResourceId, final ArrayList<Artist> artists) {
			super(context, textViewResourceId);
			this.mContext = context;
			this.mData = artists;
			if(artists == null) this.mData = new ArrayList<Artist>();

			mAlphaIndexer = new HashMap<String, Integer>();
			int size = this.mData.size();

			for (int i = size - 1; i >= 0; i--) {
	            String element = this.mData.get(i).getName().toUpperCase();
	            mAlphaIndexer.put(element.substring(0, 1), i);
	        }
			
			Set<String> sectionLetters = mAlphaIndexer.keySet();
			Iterator<String> it = sectionLetters.iterator();
			ArrayList<String> sectionList = new ArrayList<String>();
			while (it.hasNext()) {
				String key = it.next();
				sectionList.add(key);
			}
			
			Collections.sort(sectionList);
			mSections = new String[sectionList.size()];
			sectionList.toArray(mSections);
			
			mBch = new BitmapConcurrencyHandler(context);
		}
		
		public int getPositionForSection(int section) {
			if(mAlphaIndexer.isEmpty()) return 0;
	        return mAlphaIndexer.get(mSections[section]);
	    }

		public int getSectionForPosition(int position) {
	    	String ch = mData.get(position).getName().substring(0, 1).toUpperCase();
	    	for(int i = 0; i < mSections.length; i++) {
	    		if(mSections[i].equals(ch)) {
	    			return i;
	    		}
	    	}
	        return 0;
	    }

	    public Object[] getSections() {
	         return mSections;
	    }
		
		@Override
		public int getCount() {
			return mData.size();
		}
		
		@Override
		public Artist getItem(int position) {
			return mData.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			Artist artist = getItem(position);

			if(convertView == null ) {
				convertView = View.inflate(mContext, R.layout.item_collection_artist, null);
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.artist_item_title);
				holder.info = (TextView) convertView.findViewById(R.id.artist_item_album_song);
				holder.photo = (ImageView) convertView.findViewById(R.id.artist_item_cover);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			if((mContext.getResources().getBoolean(R.bool.IsTablet) &&
                    mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) ||
                    mContext.getResources().getBoolean(R.bool.IsSmallTablet)) {
				String color = (position % 4 == 0 || position % 4 == 3) ? "#cc000000" : "#aa000000";
				convertView.setBackgroundColor(Color.parseColor(color));
			} else {
				convertView.setBackgroundColor(Color.parseColor((position % 2 == 0) ? "#cc000000" : "#aa000000"));
			}
					
			holder.title.setText(mData.get(position).getName());
			int albums = artist.getAlbums();
			holder.info.setText(mData.get(position).getSongs()+" songs on "+ albums + ((albums > 1) ? " albums" : " album"));

			mBch.loadBitmap(artist.getName() + "_mini", holder.photo);
			
			return convertView;
		}
		
		@Override
		public void notifyDataSetChanged() {
			mEmptyViewText.setText(getString(PlayingState.state == PlayingState.DOWN ? R.string.collection_no_amarok : R.string.collection_no_music));
			mEmptyViewText.setVisibility(mData.isEmpty() ? View.VISIBLE : View.GONE);
			mEmptyView.setVisibility(mData.isEmpty() ? View.VISIBLE : View.GONE);
			super.notifyDataSetChanged();
		}
			
		
		private class ViewHolder {
			public TextView title;
			public ImageView photo;
			public TextView info;
		}

	}
	
	
	
	public class FillArtistsAsync extends AsyncTask<Void, Void, ArrayList<Artist>> {

		private static final String GET_ARTISTS = "/getCollectionAllArtistsJSON/";

		public FillArtistsAsync() {
            isListLoaded = true;
        }
		
		@Override
		protected ArrayList<Artist> doInBackground(Void... params) {
            ArrayList<Artist> mData = new ArrayList<>();
			try {
				String artistsJSON = CommonHelper.getStringFromHttp(Prefs.getIp() + GET_ARTISTS);
				mListAdapter = new ArtistAdapter(getActivity(), android.R.layout.simple_list_item_1, mData);
				
				JSONArray array = new JSONArray(artistsJSON);
				for(int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					String name = obj.getString("name");
					int id = obj.getInt("id"),
						songs = obj.getInt("tracks"),
						albums = obj.getInt("albums");
					mData.add(new Artist(id, name, songs, albums));
				}
			} catch (JSONException e) {
				cancel(true);
				return mData;
			} catch (NullPointerException e) {
				return mData;
			}
			
			Collections.sort(mData);
			return mData;
		}
		
		@Override
		protected void onCancelled() {
            isListLoaded = false;
            try {
                mListView.setAdapter(mListAdapter);
                mListAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.onCancelled();
        }

        @Override
		protected void onPostExecute(ArrayList<Artist> result) {
			try {
                // this will run only once per app lifecycle
                new GetCollectionPhotosTask(result, ((BaseActivity)getActivity())).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);

				mListView.setAdapter(mListAdapter);
				mListAdapter.notifyDataSetChanged();
                isListLoaded = true;
            } catch (Exception ex) { ex.printStackTrace(); }
		}
		
	}
	
	
	
}
