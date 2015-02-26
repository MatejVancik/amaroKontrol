package com.mv2studio.amarok.kontrol.ui.Fragment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.communication.amarok.AmarokCommand;
import com.mv2studio.amarok.kontrol.helpers.BitmapConcurrencyHandler;
import com.mv2studio.amarok.kontrol.helpers.CommonHelper;
import com.mv2studio.amarok.kontrol.shared.model.Song;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SearchFragment extends BaseFragment {
	
	private ListView mListView;
	private EditText mSearchEditText;
    private static final String SEARCHTAG = "/getCollectionSearchAllJSON/";
	private static SearchAdapter mSearchAdapter;
    private BitmapConcurrencyHandler mBch;
	protected Typeface tCondBold;
	private TextView mEmptyListMessage;
	
	private String saveTag = "save",
				   editTextTag = "text",
				   scrollTag = "scroll";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = getActivity();
		
		tCondBold = Typeface.createFromAsset(context.getAssets(), "fonts/cbold.ttf");

		RelativeLayout rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_search, null);
		mListView = (ListView) rootView.findViewById(R.id.fragment_search_list);
		mSearchEditText = (EditText) rootView.findViewById(R.id.fragment_search_edittext);
        ImageButton button = (ImageButton) rootView.findViewById(R.id.fragment_search_button);
		mEmptyListMessage = (TextView) rootView.findViewById(R.id.fragment_search_not_found);
        mListView.setEmptyView(mEmptyListMessage);
		mSearchEditText.setTypeface(tCondBold);
		mSearchEditText.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                new SearchTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
                editTextLooseFocus();
            }
            return false;
            }
        });
		
		button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new SearchTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
                editTextLooseFocus();
            }
        });
		
		mBch = new BitmapConcurrencyHandler(context);

		for (int i = 0; i < rootView.getChildCount(); i++) {
            View innerView = rootView.getChildAt(i);
            if(!(innerView instanceof EditText)) {
            	innerView.setOnTouchListener(new OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                    	editTextLooseFocus();
                        return false;
                    }
                });
            }
        }

		mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Song clicked = (Song) arg0.getItemAtPosition(arg2);
                AmarokCommand.COLLECTION_ENQUEUE.execute(clicked.getId() + "");
            }
        });

		return rootView;
	}
	
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
			      Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
	}
	
	private void editTextLooseFocus() {
		mSearchEditText.setFocusable(false);
		mSearchEditText.setFocusable(true);
		mSearchEditText.setFocusableInTouchMode(false);
		mSearchEditText.setFocusableInTouchMode(true);
    	hideKeyboard();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(saveTag, true);
		outState.putString(editTextTag, mSearchEditText.getText().toString());
		outState.putInt(scrollTag, mListView.getFirstVisiblePosition());
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		boolean restoreAdapter = false;
		if(savedInstanceState != null) {
			restoreAdapter = savedInstanceState.getBoolean(saveTag);
			mSearchEditText.setText(savedInstanceState.getString(editTextTag, ""));
			mListView.setSelection(savedInstanceState.getInt(scrollTag, 0));
		}
		if(!restoreAdapter) mSearchAdapter = new SearchAdapter(getActivity(), android.R.layout.simple_list_item_1, null);
		mListView.setAdapter(mSearchAdapter);
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onPause() {
		hideKeyboard();
		super.onPause();
	}
	
	
	
	
	/**
	 * ***************************************************************************************
	 * ***********************************  A D A P T E R  ***********************************
	 * ***************************************************************************************
	 */
	
	class SearchAdapter extends ArrayAdapter<Song>{
		
		ArrayList<Song> data;
		Context context;
		LayoutInflater inflater;

		public SearchAdapter(Context context, int resource, List<Song> objects) {
			super(context, resource, objects);
			this.context = context;
			data = new ArrayList<Song>();
			if(objects != null) data.addAll(objects);
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		@Override
		public int getCount() {
			return data.size();
		}
		
		@Override
		public Song getItem(int position) {
			return data.get(position);
		}
		
		@Override
		public void addAll(Collection<? extends Song> collection) {
			if(collection != null)
				data.addAll(collection);
		}
		
		@Override
		public void clear() {
			data.clear();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			Song song;
			String searchText = mSearchEditText.getText().toString();
			try {
				song = getItem(position);
			} catch (IndexOutOfBoundsException ex) { // asynctask setting data empty can cause this
				song = new Song("", "", "");
			}
			String[] data = {song.getTitle(), song.getArtist(), song.getAlbum()};
			
			for(int i = 0; i < data.length; i++) {
				if(data[i].toUpperCase().contains(searchText.toUpperCase())) {
					String[] toFormat = data[i].split("(?i)"+searchText);
					String formated = "";
					for(int j = 0; j < toFormat.length; j++) {
						formated += toFormat[j];
						
						if(j < toFormat.length-1)
							formated += "<font color='#ffffff'><b>"+searchText+"</b></font>";
					}
					data[i] = formated;
				}
			}
			
			if(convertView == null) {
				convertView= inflater.inflate(R.layout.item_search_result, null);
				holder = new ViewHolder();
				convertView.setTag(holder);
				holder.cover = (ImageView) convertView.findViewById(R.id.search_item_cover);
				holder.album = (TextView) convertView.findViewById(R.id.search_item_album);
				holder.title = (TextView) convertView.findViewById(R.id.search_item_title);
				holder.artist = (TextView) convertView.findViewById(R.id.search_item_artist);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			mBch.loadBitmap(song.getArtist() + "_mini", holder.cover);
			convertView.setBackgroundColor((position % 2 == 1) ? Color.parseColor("#00000000") : Color.parseColor("#22000000") );
			holder.title.setText(Html.fromHtml(data[0]));
			holder.artist.setText(Html.fromHtml(data[1]));
			holder.album.setText(Html.fromHtml(data[2]));
			
			return convertView;
		}
		
	}
	
	class ViewHolder {
		public ImageView cover;
		public TextView album, artist, title;
	}
	
	
	class SearchTask extends AsyncTask<Void, Void, Void> {

		private ArrayList<Song> songs = new ArrayList<Song>();
		private boolean beMoreSpecific;
		
		@Override
		protected Void doInBackground(Void... params) {
			String searchText = mSearchEditText.getText().toString();
			String json = CommonHelper.getStringFromHttp(Prefs.getIp() + SEARCHTAG + searchText);
			
			
			try {
				JSONArray array = new JSONArray(json);
				
				for(int i = 0; i < array.length(); i++) {
					JSONObject songObj = array.getJSONObject(i);
					String artistName = songObj.getString("artistName"),
						   albumName = songObj.getString("albumName"),
						   track = songObj.getString("track");
					int trackID = songObj.getInt("trackID");
					
					Song song = new Song(trackID, track, artistName, albumName);
					songs.add(song);
				}
				
			} catch (JSONException e) {
				beMoreSpecific = true;
			}
			mSearchAdapter.clear();
			mSearchAdapter.addAll(songs);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(songs.size() == 0) {
				if(beMoreSpecific) {
                    mEmptyListMessage.setText(R.string.search_too_many);
				} else {
                    mEmptyListMessage.setText(R.string.search_not_found);
				}
			}
			mSearchAdapter.notifyDataSetChanged();
			super.onPostExecute(result);
		}
		
	}
	
	
}
