package com.mv2studio.amarok.kontrol.communication;

import android.content.Context;

import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.communication.amarok.PlaylistUpdateCallback;
import com.mv2studio.amarok.kontrol.shared.PlayingState;
import com.mv2studio.amarok.kontrol.shared.model.Song;

/**
 * Created by matej on 16.11.14.
 */
public abstract class Connector {

    /**
     * Occurs when device can not try to connect to remote server.
     * (wifi/3g is down, ...)
     */
    public static final int ERROR_NOT_POSSIBLE_TO_CONNECT = -1;

    /**
     * Occurs when device is able to communicate, but does not get
     * any response from remote server.
     */
    public static final int ERROR_PLAYER_NOT_AVAILABLE = -2;

    /**
     * Occurs when playback is stopped and server sends no data
     */
    public static final int ERROR_PLAYER_STOPPED = -3;

    protected ConnectorUpdateCallback mConnectorUpdateCallback;
    protected Context mContext;

    public Connector(Context context) {
        mContext = context;
    }

    public abstract Runnable getUpdateTask();

    public abstract Runnable getConnectionUnavailableTask();

    public abstract void updatePlaylist(PlaylistUpdateCallback callback);

    public abstract Song getCurrentSong();

    public void setConnectorCallback(ConnectorUpdateCallback callback) {
        mConnectorUpdateCallback = callback;
    }

    public void notifyConnectionUnavailable() {

        PlayingState.state = PlayingState.DOWN;

        ConnectionMessage noConnectionMessage = new ConnectionMessage(
                mContext.getString(R.string.please_turn_on_wifi),
                mContext.getString(R.string.enable_3g), "");

        mConnectorUpdateCallback.onDataUnavailable(ERROR_NOT_POSSIBLE_TO_CONNECT, noConnectionMessage);
    }

    public abstract void sendCommand(Command command);

}
