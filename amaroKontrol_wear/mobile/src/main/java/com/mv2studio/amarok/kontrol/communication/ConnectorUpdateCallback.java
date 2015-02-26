package com.mv2studio.amarok.kontrol.communication;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import com.mv2studio.amarok.kontrol.shared.PlayingState;
import com.mv2studio.amarok.kontrol.shared.model.Song;

/**
 * Created by matej on 16.11.14.
 */
public abstract class ConnectorUpdateCallback extends ResultReceiver {

    public static final int RC_DATA_UPDATED = 0;
    public static final int RC_DATA_UNAVAILABLE = 1;
    public static final int RC_PLAYING_STATE_CHANGED = 2;
    public static final int RC_PLAYLIST_UPDATED = 3;

    public static final String BUNDLE_SONG = "SONG";
    public static final String BUNDLE_STATE = "STATE";
    public static final String BUNDLE_ERROR_CODE = "ERROR_CODE";
    public static final String BUNDLE_CONNECTION_MESSAGE = "CONNECTION_MESSAGE";

    public ConnectorUpdateCallback() {
        super(new Handler());
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        switch (resultCode) {
            case RC_DATA_UPDATED:
                onDataUpdated((Song)resultData.getParcelable(BUNDLE_SONG),
                        PlayingState.values()[resultData.getInt(BUNDLE_STATE)]);
                break;
            case RC_DATA_UNAVAILABLE:
                onDataUnavailable(resultData.getInt(BUNDLE_ERROR_CODE),
                        (ConnectionMessage)resultData.getParcelable(BUNDLE_CONNECTION_MESSAGE));
                break;
            case RC_PLAYING_STATE_CHANGED:
                onPlayingStateChanged(PlayingState.values()[resultData.getInt(BUNDLE_STATE)]);
                break;

        }
    }

    /**
     *  Called in update loop on successfull data update.
     *  Also called in case of
     * @param song Currently playing song. Can be null
     * @param state Playback state
     */
    public abstract void onDataUpdated(Song song, PlayingState state);

    /**
     * Called in update loop in case of error
     * @param errorCode code describing error
     * @param connectionMessage
     */
    public abstract void onDataUnavailable(int errorCode, ConnectionMessage connectionMessage);

    /**
     * Called if playing state has changed
      * @param state new playing state
     */
    public abstract void onPlayingStateChanged(PlayingState state);

}
