package com.mv2studio.amarok.kontrol.communication.amarok;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.communication.Command;
import com.mv2studio.amarok.kontrol.communication.CommandCallback;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by matej on 17.11.14.
 */
public enum AmarokCommand implements Command {

    NEXT("/cmdNext/"), PREV("/cmdPrev/"), PLAY("/cmdPlay/"), PAUSE("/cmdPause/"),
    PLAY_PAUSE("/cmdPlayPause/"), STOP("/cmdStop/"), VOL_UP("/cmdVolumeUp/"),
    VOL_DOWN("/cmdVolumeDown/"), MUTE("/cmdMute/"), PLAY_BY_INDEX("/cmdPlayByIndex/"),
    REMOVE_BY_INDEX("/cmdRemoveByIndex/"), SET_POSITION("/cmdSetPosition/"),
    COLLECTION_ENQUEUE("/cmdCollectionEnqueue/"), PLAYLIST_CLEAR("/cmdPlaylistClear/");

    private String mCommand;
    private String mParameter = "";
    private CommandCallback mCallback;

    private AmarokCommand(String command) {
        this.mCommand = command;
    }

    @Override
    public String getCommand() {
        return mCommand;
    }

    public Command param(String param) {
        if (param != null) mParameter = param;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        execute("");
    }

    public void execute(String params) {
        String url = Prefs.getIp();
        if (TextUtils.isEmpty(url)) return;

        OkHttpClient client = new OkHttpClient();
        String com = url + mCommand + mParameter + params;
        Request request = new Request.Builder().url(com).build();
        Log.e("", "COMMAND: " + com);
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Request request, IOException e) {
                runCallbackInMainThread();
                Log.e("", request.toString());
            }
            @Override public void onResponse(Response response) throws IOException {
                runCallbackInMainThread();
                Log.e("", response.toString());
            }
        });
    }

    private void runCallbackInMainThread() {
        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onResponse();
                }
            });
        }
    }

    @Override
    public Command withCallback(CommandCallback callback) {
        mCallback = callback;
        return this;
    }
}
