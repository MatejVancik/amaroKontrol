package com.mv2studio.amarok.kontrol.ui.Fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.mv2studio.amarok.kontrol.PlaylistAdapter;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.communication.CommandCallback;
import com.mv2studio.amarok.kontrol.communication.amarok.AmarokCommand;
import com.mv2studio.amarok.kontrol.helpers.CommonHelper;
import com.mv2studio.amarok.kontrol.shared.model.Song;
import com.mv2studio.amarok.kontrol.tasks.FillArtistDetailTask;
import com.mv2studio.amarok.kontrol.ui.Activity.BaseActivity;
import com.mv2studio.amarok.kontrol.ui.View.CustomTextView;

import java.util.ArrayList;

public class ArtistFragment extends BaseFragment {

    public static final String ARG_ARTIST_ID = "id";

    private ListView mListView;
    private ArtistDetailAdapter mArtistDetailAdapter;
    View header;

    private static int sLastArtistId = -1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		RelativeLayout rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_artist_detail, null);
		mListView = (ListView) rootView.findViewById(R.id.fragment_artist_detail_list);
		mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Song clicked = (Song) arg0.getItemAtPosition(arg2);

                if (clicked == null) {
                    return;
                }

                View check = arg1.findViewById(R.id.listSongAddedIcon);
                check.clearAnimation();
                check.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_out));
                AmarokCommand.COLLECTION_ENQUEUE.withCallback(new CommandCallback() {
                    @Override
                    public void onResponse() {
                        ((BaseActivity) getActivity()).refreshPlaylist();
                    }
                }).execute(clicked.getId() + "");
            }
        });

        // add header to list
        header = View.inflate(getActivity(), R.layout.artist_detail_header, null);
        mListView.addHeaderView(header);
        mListView.setTag(header);
        ((ImageView) header.findViewById(R.id.artist_detail_header_photo))
                .setMaxHeight(CommonHelper.getScreenSize(getActivity())[1] / 2);

        mArtistDetailAdapter = new ArtistDetailAdapter(getActivity(), new ArrayList<Song>());
        mListView.setAdapter(mArtistDetailAdapter);

        return rootView;
	}

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        header.setVisibility(View.INVISIBLE);

        if (!hidden) {
            int artistId = getBundle() == null ? sLastArtistId : getBundle().getInt(ARG_ARTIST_ID);
            new FillArtistDetailTask(mListView, sLastArtistId = artistId) {
                @Override
                protected void onPostExecute(ArrayList<Song> result) {
                    super.onPostExecute(result);
                    Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
                    fadeIn.setDuration(300);
                    fadeIn.setFillAfter(true);
                    header.clearAnimation();
                    header.startAnimation(fadeIn);

                    mArtistDetailAdapter.addAll(result);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        } else {
            mArtistDetailAdapter.clear();
            mArtistDetailAdapter.notifyDataSetChanged();
        }
    }

    public static class ArtistDetailAdapter extends PlaylistAdapter {

        private CommandCallback playlistRefreshCallback;

		public ArtistDetailAdapter(Context context, ArrayList<Song> objects) {
			super(context, 0, objects);
            playlistRefreshCallback = new CommandCallback() {
                @Override
                public void onResponse() {
                    mBaseActivity.refreshPlaylist();
                }
            };
        }

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			final Song song = getItem(position);

			if (convertView == null) {
				holder = new ViewHolder();
				int type = getItemViewType(position);
				switch (type) {
				case ALBUM:
					convertView = mInflater.inflate(R.layout.item_artist_detail_album, null);
					holder.cover = (ImageView) convertView.findViewById(R.id.listSongCover);
					holder.album = (CustomTextView) convertView.findViewById(R.id.listSongAlbum);
					holder.albumHigh = (Button) convertView.findViewById(R.id.detail_highlight);
					holder.songHigh = (Button) convertView.findViewById(R.id.detail_song_highlight);
					holder.song = song;
					holder.album.setTypeface(CustomTextView.TextFont.BOLD_C);
                    holder.addedAlbumIcon = (ImageView) convertView.findViewById(R.id.listAlbumAddedIcon);
                    break;
				case SONG:
					convertView = mInflater.inflate(R.layout.item_artist_detail_song, null);
					break;
				}
				holder.title = (CustomTextView) convertView.findViewById(R.id.listSongTitle);
                holder.addedSongIcon = (ImageView) convertView.findViewById(R.id.listSongAddedIcon);
                convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

            holder.addedSongIcon.clearAnimation();

			if (getItemViewType(position) == ALBUM) {
                holder.addedAlbumIcon.clearAnimation();
                holder.album.setText(song.getAlbum());
                mBch.loadBitmap(song.getArtist() + song.getAlbum(), holder.cover);

                holder.albumHigh.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String songsId = "";
                        for (Song taped : mData) {
                            if (taped.getAlbum().equals(song.getAlbum())) {
                                songsId += taped.getId() + "/";
                            }
                        }
                        songsId = songsId.subSequence(0, songsId.length() - 1).toString();

                        AmarokCommand.COLLECTION_ENQUEUE
                                .withCallback(playlistRefreshCallback)
                                .execute(songsId);

                        holder.addedAlbumIcon.startAnimation(
                                AnimationUtils.loadAnimation(mBaseActivity, R.anim.fade_in_out));
                    }
                });

                holder.songHigh.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AmarokCommand.COLLECTION_ENQUEUE
                                .withCallback(playlistRefreshCallback)
                                .execute(song.getId() + "");

                        holder.addedSongIcon.startAnimation(AnimationUtils.loadAnimation(mBaseActivity, R.anim.fade_in_out));
                    }
                });
            }
            convertView.setBackgroundColor(Color.parseColor((position % 2 == 0) ? "#cc000000" : "#aa000000"));
			holder.title.setText(song.getTitle());

			return convertView;
		}
	}
}
