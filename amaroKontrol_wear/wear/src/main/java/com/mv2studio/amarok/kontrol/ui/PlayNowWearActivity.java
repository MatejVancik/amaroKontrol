package com.mv2studio.amarok.kontrol.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.WearApp;
import com.mv2studio.amarok.kontrol.communication.Commander;
import com.mv2studio.amarok.kontrol.communication.DataLayerListenerService;
import com.mv2studio.amarok.kontrol.shared.MessageResultCallback;
import com.mv2studio.amarok.kontrol.shared.Constants;
import com.mv2studio.amarok.kontrol.shared.Log;
import com.mv2studio.amarok.kontrol.shared.PlayingState;

public class PlayNowWearActivity extends Activity implements WatchViewStub.OnLayoutInflatedListener,
        View.OnClickListener, NodeApi.NodeListener {

    private TextView mArtistText, mSongText;
    private ImageButton mPlayButton, mNextButton, mPrevButton;
    private ImageView coverView;

    private Node node;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mGoogleApiClient = WearApp.getInstance().getGoogleApiClient();
        mGoogleApiClient.connect();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(this);
    }

    @Override
    public void onLayoutInflated(WatchViewStub stub) {
        mPlayButton = (ImageButton) stub.findViewById(R.id.playPause);
        mNextButton = (ImageButton) stub.findViewById(R.id.next);
        mPrevButton = (ImageButton) stub.findViewById(R.id.prev);

        mArtistText = (TextView) stub.findViewById(R.id.artist_text);
        mSongText = (TextView) stub.findViewById(R.id.song_text);

        coverView = (ImageView) findViewById(R.id.watchBackground);

        mPlayButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mPrevButton.setOnClickListener(this);

        String artistData = getIntent().getStringExtra(DataLayerListenerService.DATA_ARTIST);
        String songData = getIntent().getStringExtra(DataLayerListenerService.DATA_SONG);

        if (artistData != null) mArtistText.setText(artistData);
        if(songData != null) mSongText.setText(songData);

        Bitmap cover = getIntent().getParcelableExtra(DataLayerListenerService.DATA_COVER_BLURED);
        if(cover == null) {
            coverView.setImageResource(R.drawable.albumblured);
        } else {
            coverView.setImageBitmap(cover);
        }
    }

    @Override
    public void onClick(final View v) {
        String command = "";
        switch (v.getId()) {
            case R.id.playPause:
                command = Constants.WEAR_COMMAND_PLAY;
                break;
            case R.id.next:
                command = Constants.WEAR_COMMAND_NEXT;
                break;
            case R.id.prev:
                command = Constants.WEAR_COMMAND_PREV;
                break;
        }
        Commander.getInstance().sendCommand(command, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("Pause: "+this.toString());

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.e("Resume: "+this.toString());

        IntentFilter filter = new IntentFilter();
        filter.addAction(DataLayerListenerService.UPDATE_PLAYBACK_STATE_ACTION);
        filter.addAction(DataLayerListenerService.UPDATE_SONG_DATA_ACTION);
        filter.addAction(DataLayerListenerService.UPDATE_UNAVAILABLE_DATA_ACTION);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        // start phone app
        sendStartCommand();
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d("PlayNowActivity received action: " + action);

            if (action.equals(DataLayerListenerService.UPDATE_SONG_DATA_ACTION)) {
                onReceiveUpdateData(intent);
            } else if (action.equals(DataLayerListenerService.UPDATE_UNAVAILABLE_DATA_ACTION)) {
                onReceiveDataUnavailable(intent);
            } else if (action.equals(DataLayerListenerService.UPDATE_PLAYBACK_STATE_ACTION)) {
                onReceivePlayingStateChanged(intent);
            }
        }
    };

    private void onReceiveUpdateData(Intent intent) {
        String artist = intent.getStringExtra(DataLayerListenerService.DATA_ARTIST);
        String song = intent.getStringExtra(DataLayerListenerService.DATA_SONG);

        Bitmap coverBlured = intent.getParcelableExtra(DataLayerListenerService.DATA_COVER_BLURED);

        // update playback button
        onReceivePlayingStateChanged(intent);

        if (coverBlured != null) {
            coverView.setImageBitmap(coverBlured);
        }

        Log.e("Notification: " + this.toString() + "Artist: " + artist + "  Song: " + song);

        mArtistText.setText(artist);
        mSongText.setText(song);
    }

    private void onReceiveDataUnavailable(Intent intent) {
        String artist = intent.getStringExtra(DataLayerListenerService.DATA_ARTIST);
        String song = intent.getStringExtra(DataLayerListenerService.DATA_SONG);

        mArtistText.setText(artist);
        mSongText.setText(song);
    }

    private void onReceivePlayingStateChanged(Intent intent) {
        PlayingState state =
                PlayingState.values()[intent.getIntExtra(DataLayerListenerService.DATA_PLABACK_STATE, 0)];
        setPlayBackButton(state);
    }

    private void setPlayBackButton(PlayingState state) {
        switch (state) {
            case DOWN:
            case NOTPLAYING:
            case PAUSE:
                mPlayButton.setImageResource(R.drawable.play_act);
                break;
            case PLAYING:
                mPlayButton.setImageResource(R.drawable.pause_act);
                break;
        }
    }


    @Override
    public void onPeerConnected(Node node) {
        sendStartCommand();
    }

    @Override
    public void onPeerDisconnected(Node node) {

    }

    private void sendStartCommand() {
        Commander.getInstance().sendCommand(Constants.COMM_START, null);
    }
}
