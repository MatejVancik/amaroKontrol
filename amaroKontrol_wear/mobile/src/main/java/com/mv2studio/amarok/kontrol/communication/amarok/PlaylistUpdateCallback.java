package com.mv2studio.amarok.kontrol.communication.amarok;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import com.mv2studio.amarok.kontrol.shared.model.Song;

import java.util.ArrayList;

/**
 * Created by matej on 18.11.14.
 */
public abstract class PlaylistUpdateCallback extends ResultReceiver {

    public static final int RC_PLAYLIST_UPDATED = 0;

    public static final String DATA_PLAYLIST = "PLAYLIST";

    public PlaylistUpdateCallback() {
        super(new Handler());
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);

        if (resultCode == RC_PLAYLIST_UPDATED) {
            onPlaylistReceived(resultData.<Song>getParcelableArrayList(DATA_PLAYLIST));
        }
    }

    public abstract void onPlaylistReceived(ArrayList<Song> list);
}
