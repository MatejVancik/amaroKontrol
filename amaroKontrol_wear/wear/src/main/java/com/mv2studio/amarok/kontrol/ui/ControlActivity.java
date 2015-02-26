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

import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.communication.Commander;
import com.mv2studio.amarok.kontrol.communication.DataLayerListenerService;
import com.mv2studio.amarok.kontrol.shared.Constants;
import com.mv2studio.amarok.kontrol.shared.Log;
import com.mv2studio.amarok.kontrol.shared.PlayingState;

/**
 * Created by matej on 15.2.15.
 */
public class ControlActivity extends Activity implements WatchViewStub.OnLayoutInflatedListener,
        View.OnClickListener {

    private View mVolUpButton, mVolDownButton, mPrevButton, mNextButotn;
    private ImageButton mPlayButton;
    private ImageView background;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        Log.e("On create control acitivty");

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.activity_control_stub);
        stub.setOnLayoutInflatedListener(this);
    }

    @Override
    public void onClick(View v) {
        String command = "";
        switch (v.getId()) {
            case R.id.vol_up:
                command = Constants.WEAR_COMMAND_VOL_UP;
                break;
            case R.id.vol_down:
                command = Constants.WEAR_COMMAND_VOL_DOWN;
                break;
            case R.id.prev:
                command = Constants.WEAR_COMMAND_PREV;
                break;
            case R.id.next:
                command = Constants.WEAR_COMMAND_NEXT;
                break;
            case R.id.playPause:
                command = Constants.WEAR_COMMAND_PLAY;
                break;
        }
        Commander.getInstance().sendCommand(command, null);
    }

    @Override
    public void onLayoutInflated(WatchViewStub watchViewStub) {
        Log.e("control stub inflated");
        background = (ImageView) findViewById(R.id.watchBackground);

        mVolUpButton = watchViewStub.findViewById(R.id.vol_up);
        mVolDownButton = watchViewStub.findViewById(R.id.vol_down);
        mPrevButton = watchViewStub.findViewById(R.id.prev);
        mNextButotn = watchViewStub.findViewById(R.id.next);
        mPlayButton = (ImageButton) watchViewStub.findViewById(R.id.playPause);

        mVolUpButton.setOnClickListener(this);
        mVolDownButton.setOnClickListener(this);
        mPrevButton.setOnClickListener(this);
        mNextButotn.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);

        updateBackground(getIntent().<Bitmap>getParcelableExtra(DataLayerListenerService.DATA_COVER_BLURED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("control paused");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.e("control resumed");

        IntentFilter filter = new IntentFilter();
        filter.addAction(DataLayerListenerService.UPDATE_PLAYBACK_STATE_ACTION);
        filter.addAction(DataLayerListenerService.UPDATE_SONG_DATA_ACTION);
        filter.addAction(DataLayerListenerService.UPDATE_UNAVAILABLE_DATA_ACTION);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DataLayerListenerService.UPDATE_SONG_DATA_ACTION)) {
                Bitmap coverBlured = intent.getParcelableExtra(DataLayerListenerService.DATA_COVER_BLURED);
                updateBackground(coverBlured);
            } else if (action.equals(DataLayerListenerService.UPDATE_UNAVAILABLE_DATA_ACTION)) {
                updateBackground(null);
            } else if (action.equals(DataLayerListenerService.UPDATE_PLAYBACK_STATE_ACTION)) {
                PlayingState state =
                        PlayingState.values()[intent.getIntExtra(DataLayerListenerService.DATA_PLABACK_STATE, 0)];
                setPlayBackButton(state);
            }
        }
    };

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

    private void updateBackground(Bitmap cover) {
        if(cover == null) {
            background.setImageResource(R.drawable.albumblured);
        } else {
            background.setImageBitmap(cover);
        }
    }

}
