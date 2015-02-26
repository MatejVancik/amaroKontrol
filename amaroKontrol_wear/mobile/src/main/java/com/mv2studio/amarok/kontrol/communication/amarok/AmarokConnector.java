package com.mv2studio.amarok.kontrol.communication.amarok;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.communication.Command;
import com.mv2studio.amarok.kontrol.communication.ConnectionMessage;
import com.mv2studio.amarok.kontrol.communication.Connector;
import com.mv2studio.amarok.kontrol.helpers.CommonHelper;
import com.mv2studio.amarok.kontrol.helpers.MediaHelper;
import com.mv2studio.amarok.kontrol.shared.Log;
import com.mv2studio.amarok.kontrol.shared.PlayingState;
import com.mv2studio.amarok.kontrol.shared.model.Song;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by matej on 16.11.14.
 */
public class AmarokConnector extends Connector {

    public static final int ERROR_OLD_SCRIPT = 1;

    private static final String URL_CURRENT_SONG = "/getCurrentSongJson/";
    private static final String URL_GET_LYRICS = "/getLyrics/";
    private static final String URL_GET_COVER = "/getCurrentCover/" + Prefs.getScreenWidth();
    private static final String URL_GET_PLAYLIST = "/getPlaylistJSON/";

    private int ignoreCount = 0;
    private static final int IGNORE_TOTAL = 2;

    private Song currentSong;

    public AmarokConnector(Context context) {
        super(context);
    }

    @Override
    public Runnable getUpdateTask() {
        return mUpdateTask;
    }

    @Override
    public Runnable getConnectionUnavailableTask() {
        return mConnectionUnavailableTask;
    }

    @Override
    public void updatePlaylist(final PlaylistUpdateCallback callback) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Song> songsList = new ArrayList<Song>();

                // get data from amarok
                String songsJSON = CommonHelper.getStringFromHttp(Prefs.getIp() + URL_GET_PLAYLIST);

                try {
                    JSONArray songs = new JSONArray(songsJSON);
                    for(int i = 0; i < songs.length(); i++) {
                        JSONObject song = songs.getJSONObject(i);
                        String artist = song.getString("artist"),
                                title = song.getString("title"),
                                album = song.getString("album");
                        int id = song.getInt("id");
                        songsList.add(new Song(id, title, artist, album));
                    }
                } catch (JSONException e) {
                }

                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(PlaylistUpdateCallback.DATA_PLAYLIST, songsList);

                callback.send(PlaylistUpdateCallback.RC_PLAYLIST_UPDATED, bundle);
            }
        }).start();

    }

    @Override
    public Song getCurrentSong() {
        return currentSong;
    }

    @Override
    public void sendCommand(Command command) {
        command.execute();
    }

    private Runnable mConnectionUnavailableTask = new Runnable() {
        @Override
        public void run() {
            Log.e("AmarokConnector:100  -connection unavailable");
        }
    };


    private Runnable mUpdateTask = new Runnable() {
        @Override
        public void run() {
            final String address = Prefs.getIp();

            // Download data
            String json = CommonHelper.getStringFromHttp(address + URL_CURRENT_SONG);

            // NACK was returned in script v1.0 in case it wasn't playing. new script required.
            if (json.equals("NACK")) {
                if (mConnectorUpdateCallback != null) {
                    ConnectionMessage oldScriptMessage = new ConnectionMessage(
                            mContext.getString(R.string.update_script), "", "");

                    mConnectorUpdateCallback.onDataUnavailable(ERROR_OLD_SCRIPT, oldScriptMessage);
                }

                return;
            }

            String title = null, artist = null, album = null;
            int id = 0, length = 0, position = 0, playingState = 3;
            try {
                JSONObject main = new JSONObject(json);

                // get track details
                JSONObject track = main.getJSONObject("currentTrack");
                title = track.getString("title");
                artist = track.getString("artist");
                album = track.getString("album");

                id = track.getInt("id");
                length = track.getInt("length");
                position = track.getInt("position");
                playingState = track.getInt("status");

            } catch (JSONException e) { // AMAROK OFF / SOME ERROR
                playingState = 3;
            }

            boolean playingStateChanged = playingState != PlayingState.state.ordinal();

            // set current state
            switch(playingState) {
                case 0: PlayingState.state = PlayingState.PLAYING; break;
                case 1: PlayingState.state = PlayingState.PAUSE; break;
                case 2: PlayingState.state = PlayingState.NOTPLAYING; break;
                default: PlayingState.state = PlayingState.DOWN; break;
            }

            //state changed
            if(playingStateChanged && mConnectorUpdateCallback != null) {
                mConnectorUpdateCallback.onPlayingStateChanged(PlayingState.state);
            }

            switch (PlayingState.state) {
                case PLAYING:
                case PAUSE:
                    ignoreCount = 0;
                    break;
                case NOTPLAYING:
                    // DEAL WITH SCRIPT BUG (IGNORE NOTPLAYING)
                    if(ignoreCount++ < IGNORE_TOTAL) return;

                    // update state to not playing
                    if(mConnectorUpdateCallback != null)
                        mConnectorUpdateCallback.onDataUnavailable(Connector.ERROR_PLAYER_STOPPED,
                                new ConnectionMessage(
                                        mContext.getString(R.string.not_playing),
                                        mContext.getString(R.string.press_play),
                                        mContext.getString(R.string.click_playlist_item)
                                ));
                    return;

                case DOWN:
                    // update state to down
                    if(mConnectorUpdateCallback != null)
                        mConnectorUpdateCallback.onDataUnavailable(Connector.ERROR_PLAYER_NOT_AVAILABLE,
                                new ConnectionMessage(
                                        mContext.getString(R.string.not_connected),
                                        mContext.getString(R.string.amarok_not_on),
                                        mContext.getString(R.string.amarok_bad_ip)
                                ));

                    ignoreCount = 0;
                    return;

            }

            Bitmap coverArt = null;
            Bitmap bluredCover;

            Song newSong = new Song(id, title, artist, album);
            newSong.setLength(length);
            newSong.setPosition(position);

            if (newSong.equals(currentSong)){
                // bit of optimization - do not update coverArt if already have it.
                currentSong.setPosition(position);

            } else {

                // set current song / download bitmap if new song
                currentSong = newSong;
                if(PlayingState.state != PlayingState.DOWN) {
                    coverArt = MediaHelper.downloadBitmap(address + URL_GET_COVER, artist + album, mContext, true);
                }

                bluredCover = MediaHelper.getBluredBitmap(artist + album, coverArt, mContext);
                if (coverArt == null) {
                    coverArt = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.album);
                }

                currentSong.setCover(coverArt);
                currentSong.setBluredCover(bluredCover);

                // if lyrics
                if(mContext.getResources().getBoolean(R.bool.IsTablet)) {
                    String lyrics = CommonHelper.getStringFromHttp(address + URL_GET_LYRICS + currentSong.getId());
                    currentSong.setLyrics(lyrics);
                }

            }

            // send song detail broadcast
            if (mConnectorUpdateCallback != null) {
                mConnectorUpdateCallback.onDataUpdated(currentSong, PlayingState.state);
            }
        }
    };


}
