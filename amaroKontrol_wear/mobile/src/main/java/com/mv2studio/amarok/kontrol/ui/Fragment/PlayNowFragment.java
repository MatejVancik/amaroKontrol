package com.mv2studio.amarok.kontrol.ui.Fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.mv2studio.amarok.kontrol.App;
import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.communication.ConnectionMessage;
import com.mv2studio.amarok.kontrol.communication.ConnectionService;
import com.mv2studio.amarok.kontrol.communication.Connector;
import com.mv2studio.amarok.kontrol.communication.ConnectorUpdateCallback;
import com.mv2studio.amarok.kontrol.communication.amarok.AmarokCommand;
import com.mv2studio.amarok.kontrol.shared.Log;
import com.mv2studio.amarok.kontrol.shared.PlayingState;
import com.mv2studio.amarok.kontrol.shared.model.Song;
import com.mv2studio.amarok.kontrol.ui.Activity.BaseActivity;
import com.mv2studio.amarok.kontrol.ui.View.CustomTextView;

import java.util.IllegalFormatException;
import java.util.concurrent.TimeUnit;

public class PlayNowFragment extends BaseFragment {

	private ImageButton playPause;

	private boolean updateShown;

	/*********  DATA  **********/
	private String artistData, albumData, titleData, lyricsData;
	private int lengthData, positionData;
    private PlayingState state;
	private Bitmap coverData;

	/*********  DATA_TAGS  **********/
	private String artistTag = "ARTIST",
				   albumTag = "album",
				   titleTag = "title",
				   lengthTag = "length",
				   lyricsTag = "lyrics",
				   positionTag = "position",
				   updateTag = "update",
				   stateTag = "state",
				   coverTag = "cover";


	/*********  VIEWS **********/
	private CustomTextView lyricsView, position, length, title, artist, album;
	private ImageView coverView;
	private SeekBar seekBar;
	private ScrollView lyricsScrollView;
	private RelativeLayout coverLayout;

	private static float rest = 0;

    private ConnectorUpdateCallback mConnectorUpdateCallback = new ConnectorUpdateCallback() {
        @Override
        public void onDataUpdated(Song song, PlayingState currentState) {
            if (state != currentState) {
                state = currentState;
                playPause.setImageResource(state == PlayingState.PLAYING ?
                        R.drawable.pause_act : R.drawable.play_act);
            }

            if (song == null) return;

            artistData = song.getArtist();
            albumData = song.getAlbum();
            titleData = song.getTitle();
            lyricsData = song.getLyrics();

            lengthData = song.getLength();
            positionData = song.getPosition();

            title.setText(titleData);
            artist.setText(artistData);
            album.setText(albumData);

            if (lyricsView != null) {
                lyricsView.setText(lyricsData);
            }

            length.setText(getReadableTime(lengthData));
            position.setText(getReadableTime(positionData));

            if (state != currentState) {
                state = currentState;
                playPause.setImageResource(state == PlayingState.PLAYING ?
                        R.drawable.pause_act : R.drawable.play_act);
            }

            if (song.getCover() != null && song.getCover() != coverData) {
                setUpCoverView(song.getCover());
                coverData = song.getCover();
            }

            seekBar.setMax(lengthData);
            if (positionData <= lengthData)
                seekBar.setProgress(positionData);


            if (state == PlayingState.PLAYING) {
                try {
                    lyricsScrollView.scrollBy(0, getScrollingHeight(
                            lyricsScrollView, lyricsView,
                            positionData / 1000, lengthData / 1000, 20));
                } catch (NullPointerException ex) {
                }
            }

            BaseActivity ba = (BaseActivity) getActivity();
            if (ba != null) ba.updatePlaylistItemsPlayIcon();

        }

        @Override
        public void onDataUnavailable(int errorCode, ConnectionMessage connectionMessage) {
            Log.e("data unavailable");
            // default messages for default error codes
            switch (errorCode) {

                case Connector.ERROR_NOT_POSSIBLE_TO_CONNECT:
                    titleData = "";
                    artistData = getString(R.string.please_turn_on_wifi);
                    albumData = getString(R.string.enable_3g);
                    break;

                case Connector.ERROR_PLAYER_NOT_AVAILABLE:
                    titleData = getString(R.string.not_connected);
                    artistData = getString(R.string.amarok_not_on);
                    albumData = getString(R.string.amarok_bad_ip);
                    break;

            }

            // in case connection message is included, use it
            if (connectionMessage != null) {
                titleData = connectionMessage.getTopLine();
                artistData = connectionMessage.getMiddleLine();
                albumData = connectionMessage.getBottomLine();
            }

            title.setText(titleData);
            artist.setText(artistData);
            album.setText(albumData);

            lengthData = 0;
            positionData = 0;

            length.setText(getReadableTime(lengthData));
            position.setText(getReadableTime(positionData));
            seekBar.setProgress(0);
        }

        @Override
        public void onPlayingStateChanged(PlayingState state) {
            if (state == PlayingState.DOWN) {
                setDefaultImages();
            } else {
                setUpCoverView(coverData);

                if(isHidden()) return;

                ((BaseActivity) getActivity()).recoverBackground();
            }
        }
    };


	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_play_now, container, false);

		/*********** VIEWS ************/
		seekBar = (SeekBar) view.findViewById(R.id.songSeek);
		position = (CustomTextView) view.findViewById(R.id.position);
		length = (CustomTextView) view.findViewById(R.id.length);
		title = (CustomTextView) view.findViewById(R.id.songTitle);
		artist = (CustomTextView) view.findViewById(R.id.artist);
		album = (CustomTextView) view.findViewById(R.id.albumTitle);
		coverView = (ImageView) view.findViewById(R.id.cover);
		coverLayout = (RelativeLayout) view.findViewById(R.id.relPlayNow);
		lyricsScrollView = (ScrollView) view.findViewById(R.id.lyricsScroll);
		lyricsView = (CustomTextView) view.findViewById(R.id.lyrics);


		title.setTypeface(CustomTextView.TextFont.BOLD_C);
		album.setTypeface(CustomTextView.TextFont.REG_C);
		artist.setTypeface(CustomTextView.TextFont.REG_C);
		position.setTypeface(CustomTextView.TextFont.LIGHT_C);
		length.setTypeface(CustomTextView.TextFont.LIGHT_C);
		if(lyricsView != null) lyricsView.setTypeface(CustomTextView.TextFont.REG_C);


		/*********** BUTTONS ***********/
        OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.prev:
                        App.getInstance().sendCommand(AmarokCommand.PREV);
                        break;
                    case R.id.playPause:
                        App.getInstance().sendCommand(AmarokCommand.PLAY_PAUSE);
                        break;
                    case R.id.next:
                        App.getInstance().sendCommand(AmarokCommand.NEXT);
                        break;
                }
            }
        };

		final ImageButton prev = (ImageButton) view.findViewById(R.id.prev);
		prev.setOnClickListener(clickListener);

		playPause = (ImageButton) view.findViewById(R.id.playPause);
		playPause.setOnClickListener(clickListener);

		final ImageButton next = (ImageButton) view.findViewById(R.id.next);
		next.setOnClickListener(clickListener);



		/*********** SEEKBAR ***********/
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
                App.getInstance().sendCommand(AmarokCommand.SET_POSITION
                        .param(seekBar.getProgress()+""));
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
		});

		album.setText(albumData);
		artist.setText(artistData);
		title.setText(titleData);
		seekBar.setMax(lengthData);
		seekBar.setProgress(positionData);
		coverView.setImageBitmap(coverData);
		playPause.setImageResource(state == PlayingState.PLAYING ?
                R.drawable.apause : R.drawable.aplay);


		return view;
	}

    @Override
    public void onPause() {
        super.onPause();
        App.getInstance().unregisterConnectorCallback(mConnectorUpdateCallback);
    }

    @Override
	public void onResume() {
		super.onResume();
        App.getInstance().registerConnectorCallback(mConnectorUpdateCallback);
        App.getInstance().sendBroadcast(new Intent(ConnectionService.FORCE_UPDATE));
	}

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            if (PlayingState.state == PlayingState.DOWN) {
                setDefaultImages();
            } else {
                ((BaseActivity) getActivity()).recoverBackground();
            }
        }
    }

    private void setDefaultImages() {
        coverData = BitmapFactory.decodeResource(getResources(), R.drawable.album);
        setUpCoverView(coverData);

        // update cover, but do not touch background - can be in collection or so...
        if(isHidden()) return;

        ((BaseActivity) getActivity()).imageViewAnimatedChange(
                BitmapFactory.decodeResource(getResources(), R.drawable.albumblured));
    }

    private void setUpCoverView(Bitmap coverArt) {
		coverView.setImageBitmap(coverArt);

		if(coverLayout == null) return;

		float width = coverLayout.getWidth();
		float height = coverLayout.getHeight();

		MarginLayoutParams lp = (MarginLayoutParams) coverView.getLayoutParams();

		float coverArtHeight = height - seekBar.getHeight();// - songLengthView.get().getHeight();// - additional space
		float coverArtWidth = width - lp.leftMargin - lp.rightMargin;

		float bitmapRatio;
		if(coverArt != null) {
			bitmapRatio = (float)coverArt.getWidth() / (float)coverArt.getHeight();
		} else bitmapRatio = 1;

		float spaceRatio = coverArtWidth / coverArtHeight;
		if(bitmapRatio > spaceRatio) {
			width = coverArtWidth;
			height = coverArtWidth / bitmapRatio;
		} else {
			height = coverArtHeight;
			width = coverArtHeight * bitmapRatio;
		}
		coverView.getLayoutParams().width = (int)width;
		coverView.getLayoutParams().height = (int)height;
	}

	private int getScrollingHeight(ScrollView scroll, View innerView, int current, int max, int offset) {
		if(current < offset) return 0;

		max = max - offset * 2;
		if(scroll == null || innerView == null) return 0;
		int scrollHeight = scroll.getHeight();
		int innerHeight = innerView.getHeight();
		if((scrollHeight >= innerHeight) || (max-20 <= 0)) return 0;

		int scrollingSize = innerHeight - scrollHeight;

		float toScroll = scrollingSize / (float)max * Prefs.updateInterval;
		if((rest += toScroll - Math.floor(toScroll)) >= 1) {
			rest--;
			toScroll++;
		}
		return (int)toScroll;
	}


	private String getReadableTime(int set) {
		try {
			return String.format("%d:%02d",
        	    TimeUnit.MILLISECONDS.toMinutes(set),
        	    TimeUnit.MILLISECONDS.toSeconds(set) -
        	    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(set)));
		} catch (NumberFormatException e) {
			return "0:00";
		} catch (IllegalFormatException e) {
			return "0:00";
		}
	}

	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(albumTag, albumData);
		outState.putString(artistTag, artistData);
		outState.putString(titleTag, titleData);
		outState.putInt(lengthTag, lengthData);
		outState.putInt(positionTag, positionData);
        if(state != null) outState.putInt(stateTag, state.ordinal());
		outState.putString(lyricsTag, lyricsData);
		outState.putBoolean(updateTag, updateShown);
		outState.putParcelable(coverTag, coverData);
		coverData = null;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(savedInstanceState != null) {
			albumData = savedInstanceState.getString(albumTag);
			artistData = savedInstanceState.getString(artistTag);
			titleData = savedInstanceState.getString(titleTag);
			positionData = savedInstanceState.getInt(positionTag);
			lengthData = savedInstanceState.getInt(lengthTag);
			coverData = savedInstanceState.getParcelable(coverTag);
			lyricsData = savedInstanceState.getString(lyricsTag);
			updateShown = savedInstanceState.getBoolean(updateTag);

			album.setText(albumData);
			artist.setText(artistData);
			title.setText(titleData);

			// cover dimensions needs to be counted,
			// layout if available after viewTreeObserver is triggered. (50ms should be enough)
			if(coverData != null) {
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						setUpCoverView(coverData);
					}
				}, 50);
			}

			seekBar.setMax(lengthData);
			seekBar.setProgress(positionData+1);

			length.setText(getReadableTime(lengthData));
			position.setText(getReadableTime(positionData));

			if(lyricsView != null) {
				lyricsView.setText(lyricsData);
			}

			state = PlayingState.values()[savedInstanceState.getInt(stateTag)];
		}
	}
}
