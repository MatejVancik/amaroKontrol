package com.mv2studio.amarok.kontrol;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.fortysevendeg.swipelistview.SwipeListView;
import com.mv2studio.amarok.kontrol.communication.CommandCallback;
import com.mv2studio.amarok.kontrol.communication.ConnectionMessage;
import com.mv2studio.amarok.kontrol.communication.ConnectorUpdateCallback;
import com.mv2studio.amarok.kontrol.communication.amarok.AmarokCommand;
import com.mv2studio.amarok.kontrol.helpers.BitmapConcurrencyHandler;
import com.mv2studio.amarok.kontrol.helpers.MediaHelper;
import com.mv2studio.amarok.kontrol.shared.PlayingState;
import com.mv2studio.amarok.kontrol.shared.model.Song;
import com.mv2studio.amarok.kontrol.ui.Activity.BaseActivity;
import com.mv2studio.amarok.kontrol.ui.View.CustomTextView;

import java.util.ArrayList;
import java.util.Collection;

public class PlaylistAdapter extends ArrayAdapter<Song> {

	protected static final int ALBUM = 0;
	protected static final int SONG = 1;

    protected ArrayList<Song> mData;
	protected BaseActivity mBaseActivity;
	protected LayoutInflater mInflater;

	protected BitmapConcurrencyHandler mBch;

    protected Song currentSong;


	public void replaceContext(BaseActivity context) {
		this.mBaseActivity = context;
	}

	public PlaylistAdapter(Context context, int textViewResourceId, final ArrayList<Song> songs) {
		super(context, textViewResourceId);
		this.mBaseActivity = (BaseActivity) context;
		this.mData = songs;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		try {
			songs.get(0).setNewAlbum(true);
			for(int songIndex = 1; songIndex < songs.size(); songIndex++) {
				if(!songs.get(songIndex).onSameAlbum(songs.get(songIndex-1))) {
					songs.get(songIndex).setNewAlbum(true);
				}
			}
		} catch (IndexOutOfBoundsException ex) {}

		mBch = new BitmapConcurrencyHandler(context);

        App.getInstance().registerConnectorCallback(mConnectorUpdateCallback);
	}

    public void unregisterConnectorCallback() {
        App.getInstance().unregisterConnectorCallback(mConnectorUpdateCallback);
    }

    public int getIndexOfCurrentSong() {
		for(int i = 0; i < mData.size(); i++) {
			if(mData.get(i).equals(currentSong)) return i;
		}
		return -1;
	}
	
	@Override
	public int getCount() {
		return mData.size();
	}
	
	public void removeAtPosition(int position) {
		mData.remove(position);
		this.notifyDataSetChanged();
	}
	
	@Override
	public Song getItem(int position) {
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public int getItemViewType(int position) {
		return mData.get(position).isNewAlbum() ? ALBUM : SONG;
	}
	
	public int getViewTypeCount() {
		return 2;		
	}
	
	@Override
	public void clear() {
		mData.clear();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void addAll(Collection<? extends Song> collection) {
		mData = (ArrayList<Song>) collection;
		try{ mData.get(0).setNewAlbum(true); } catch(IndexOutOfBoundsException ex) {  return; }
		for(int songIndex = 1; songIndex < collection.size(); songIndex++) {
			if(!mData.get(songIndex).onSameAlbum(mData.get(songIndex-1))) {
				mData.get(songIndex).setNewAlbum(true);
			}
		}
        notifyDataSetChanged();
	}
	
	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		ViewHolder holder;
		final Song song = getItem(position);

		if(convertView == null) {
			holder = new ViewHolder();
			int type = getItemViewType(position);
			
			switch(type) {
			case ALBUM:
				convertView = mInflater.inflate(R.layout.item_playlist_album, parent, false);
				holder.cover = (ImageView) convertView.findViewById(R.id.listSongCover);				
				holder.artist = (CustomTextView) convertView.findViewById(R.id.listSongArtist);
				holder.album = (CustomTextView) convertView.findViewById(R.id.listSongAlbum);
				holder.downloadText = (CustomTextView) convertView.findViewById(R.id.playlist_back_view_download_text);
				holder.removeText = (CustomTextView) convertView.findViewById(R.id.playlist_back_view_remove_text);

                holder.download = convertView.findViewById(R.id.playlist_back_view_download);
				holder.remove = convertView.findViewById(R.id.playlist_back_view_remove);
				break;
			case SONG:
				convertView = mInflater.inflate(R.layout.item_playlist_song, parent, false);
				holder.download = convertView.findViewById(R.id.playlist_back_view_download);
				holder.remove = convertView.findViewById(R.id.playlist_back_view_remove);
				break;
			}
			holder.backLayout = (LinearLayout) convertView.findViewById(R.id.back);
			holder.frontLayout = (RelativeLayout) convertView.findViewById(R.id.front);


            holder.playingIcon = (ImageView) convertView.findViewById(R.id.listSongPlayingIcon);
			convertView.setTag(holder);
			holder.title = (CustomTextView) convertView.findViewById(R.id.listSongTitle);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		 ((SwipeListView)parent).recycle(convertView, position);
		 ((SwipeListView)parent).closeOpenedItems();
		
		holder.frontLayout.setBackgroundColor(Color.parseColor((position % 2 == 0) ? "#77000000" : "#aa000000"));
		holder.frontLayout.setX(0f);

        holder.title.setTag(getItem(position).toString()); //TODO:null pointer

        if(getItemViewType(position) == ALBUM) {
            holder.song = song;
            holder.cover.setTag(position);

            mBch.loadBitmap(song.getArtist() + song.getAlbum() + "_mini", holder.cover);

            holder.artist.setText(song.getArtist());
            holder.album.setText(song.getAlbum());
        }

        holder.download.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SwipeListView) parent).closeOpenedItems();
                try { MediaHelper.downloadSong(song.getId(), song, mBaseActivity); } catch(Exception e) {}
            }
        });

        holder.remove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SwipeListView) parent).closeOpenedItems();

                App.getInstance().sendCommand(
                        AmarokCommand.REMOVE_BY_INDEX.param(song.getId() + "").withCallback(
                                new CommandCallback() {
                                    @Override
                                    public void onResponse() {
                                        mBaseActivity.refreshPlaylist();
                                    }
                                }));
            }
        });

        holder.backLayout.setVisibility(View.INVISIBLE);
        holder.playingIcon.setVisibility((song.equals(currentSong)) ? View.VISIBLE : View.GONE);
        holder.title.setText(song.getTitle());

		return convertView;
	}
	
	public class ViewHolder {
		public ImageView cover, playingIcon, addedSongIcon, addedAlbumIcon;
		public CustomTextView artist, album, title, removeText, downloadText;
		public Song song;
		public int position;
		public Button albumHigh;
		public Button songHigh;
		public View remove, download;
		public RelativeLayout frontLayout;
		public LinearLayout backLayout;
	}

    private final ConnectorUpdateCallback mConnectorUpdateCallback = new ConnectorUpdateCallback() {
        @Override
        public void onDataUpdated(Song song, PlayingState state) {
            currentSong = song;
        }

        @Override
        public void onDataUnavailable(int errorCode, ConnectionMessage connectionMessage) {

        }

        @Override
        public void onPlayingStateChanged(PlayingState state) {

        }
    };

}
