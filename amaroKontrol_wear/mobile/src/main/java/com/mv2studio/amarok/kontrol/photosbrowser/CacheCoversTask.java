package com.mv2studio.amarok.kontrol.photosbrowser;

import android.content.Context;
import android.os.AsyncTask;

import com.mv2studio.amarok.kontrol.App;
import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.helpers.MediaHelper;
import com.mv2studio.amarok.kontrol.shared.model.Song;

public class CacheCoversTask extends AsyncTask<Song, Void, Void> {

    private Context context;
    private static boolean inProgressOrDone;

    private CacheCoversTask() {
        context = App.getInstance();
    }

    public static void runTask(Song... songs) {
        if(inProgressOrDone) return;
        inProgressOrDone = true;
        new CacheCoversTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, songs);
    }

    protected String getCover = "/getCover/";

    @Override
    protected Void doInBackground(Song... songsList) {
        String prevAlbum = "";
        for(Song song: songsList) {
            try {
                if(prevAlbum.equals(song.getAlbum()) || (MediaHelper.readFromCache(
                        song.getArtist() + song.getAlbum() + "_mini", context) != null))
                { continue; }

                prevAlbum = song.getAlbum();

                MediaHelper.downloadBitmap(Prefs.getIp() + getCover + song.getId() + "/"
                                + context.getResources().getInteger(R.integer.playlist_cover_dimen),
                        song.getArtist() + song.getAlbum() + "_mini", context);
            } catch (NullPointerException ex) { /* Don't worry, be happy. */ }
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        inProgressOrDone = false;
    }
}