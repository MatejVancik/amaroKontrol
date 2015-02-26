package com.mv2studio.amarok.kontrol.communication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.mv2studio.amarok.kontrol.WearApp;
import com.mv2studio.amarok.kontrol.shared.Constants;
import com.mv2studio.amarok.kontrol.shared.Log;
import com.mv2studio.amarok.kontrol.shared.MessageResultCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matej on 15.2.15.
 */
public class Commander {

    public static final String APP_PCKG = "com.mv2studio.amarok.kontrol";
    public static final String COMMAND_PLAY_PAUSE = APP_PCKG + ".play";
    public static final String COMMAND_NEXT = APP_PCKG + ".next";
    public static final String COMMAND_PREV = APP_PCKG + ".prev";
    public static final String COMMAND_VOL_UP = APP_PCKG + ".vol_up";
    public static final String COMMAND_VOL_DOWN = APP_PCKG + "vol_down";

    private GoogleApiClient googleApiClient = WearApp.getInstance().getGoogleApiClient();

    private static Commander sInstance;

    private Commander() {
        googleApiClient = WearApp.getInstance().getGoogleApiClient();
        googleApiClient.connect();
    }

    public static Commander getInstance() {
        if(sInstance == null) sInstance = new Commander();

        return sInstance;
    }

    public void sendCommand(final String message, final MessageResultCallback callback) {
        Log.e("sending command " + message);
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Log.e("preexecute");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                Log.e("Runing in background");

                while (!googleApiClient.isConnected() || googleApiClient.isConnecting()) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                List<String> nodes = getNodes();
                if (nodes.size() == 0) {
                    cancel(true);
                    return false;
                }

                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                        googleApiClient, nodes.get(0), message, null).await();

                Log.e("result: "+result.toString());
                if (!result.getStatus().isSuccess()) {
                    Log.e("ERROR: failed to send Message: " + result.getStatus());
                }

                return result.getStatus().isSuccess();
            }

            @Override
            protected void onPostExecute(Boolean wasSuccessful) {
                if (callback != null)
                    callback.onMessageResult(wasSuccessful);
            }
        }.execute();
    }

    private List<String> getNodes() {
        List<String> results = new ArrayList<String>();
        Log.e("Getting nodes");
        GoogleApiClient client = WearApp.getInstance().getGoogleApiClient();
        client.connect();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(client).await();
        client.disconnect();

        Log.e("have nodes");
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
            Log.d("Node found: " + node.getDisplayName());
        }
        Log.e("have "+results.size()+"nodes");
        return results;
    }

    public static class NotificationActionCommandReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(COMMAND_PLAY_PAUSE)) {
                sInstance.sendCommand(Constants.WEAR_COMMAND_PLAY, null);
            }
        }
    }

}
