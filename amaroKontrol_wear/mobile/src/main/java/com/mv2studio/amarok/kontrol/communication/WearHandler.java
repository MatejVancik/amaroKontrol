package com.mv2studio.amarok.kontrol.communication;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.mv2studio.amarok.kontrol.App;
import com.mv2studio.amarok.kontrol.helpers.BitmapHelper;
import com.mv2studio.amarok.kontrol.shared.Constants;
import com.mv2studio.amarok.kontrol.shared.Log;
import com.mv2studio.amarok.kontrol.shared.MessageResultCallback;
import com.mv2studio.amarok.kontrol.shared.PlayingState;
import com.mv2studio.amarok.kontrol.shared.WearHelper;
import com.mv2studio.amarok.kontrol.shared.model.Song;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by matej on 19.11.14.
 */
public class WearHandler implements NodeApi.NodeListener, GoogleApiClient.ConnectionCallbacks {

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private int dumbSyncFuckerNumber;

    private int mNodesCounter;

    public WearHandler(Context context, Connector connector) {
        mContext = context;

        // connect to google client
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

        // watch for newly connected watches
        Wearable.NodeApi.addListener(mGoogleApiClient, this);

        // start app on all connected watches
        startAppOnAllConnectedNodes();
    }

    public void unregisterCallback() {
        App.getInstance().unregisterConnectorCallback(wearConnectorUpdateCallback);
    }

    public int getNodesCount() {
        return mNodesCounter;
    }

    public void updateWatch() {
        // simple incrementing can result into sending same number after instance kill.
        dumbSyncFuckerNumber = new Random().nextInt();
    }

    @Override
    public void onPeerConnected(Node node) {
        // start app on new node
        if (App.isActivityVisible() || App.isNotificationVisible()) {
            asyncStartAppOnNode(node);
        }
        mNodesCounter++;
        updateWatch();
    }

    @Override
    public void onPeerDisconnected(Node node) {
        mNodesCounter--;
    }

    public void runDataUpdateTask(final Song updatedSong, final PlayingState state) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                // create specific data map to carry all data
                PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.COMM_PATH_SONG);

                // put song info
                dataMap.getDataMap().putStringArray(Constants.COMM_DATA_CONTENT,
                        new String[]{updatedSong.getArtist(), updatedSong.getTitle(), updatedSong.getAlbum()});

                // break sync barier
                dataMap.getDataMap().putInt("almostUselessNumber", dumbSyncFuckerNumber);

                Log.d("Putting dumb fucker number " + dumbSyncFuckerNumber);

                // put playback state
                dataMap.getDataMap().putInt(Constants.COMM_DATA_STATE, state.ordinal());

                // put cover if possible
                if (updatedSong.getCover() != null) {
                    dataMap.getDataMap().putAsset(Constants.COMM_DATA_COVER,
                            WearHelper.createAssetFromBitmap(BitmapHelper.scaleToFitWidth
                                    (updatedSong.getCover(), 380, Bitmap.Config.RGB_565)));
                }

                // put blured cover if possible
                if (updatedSong.getBlured() != null) {
                    dataMap.getDataMap().putAsset(Constants.COMM_DATA_COVER_BLURED,
                            WearHelper.createAssetFromBitmap(BitmapHelper.scaleToFitWidth
                                    (updatedSong.getBlured(), 50, Bitmap.Config.RGB_565)));
                }

                sendDataMap(dataMap);

            }
        }).start();

    }

    private ConnectorUpdateCallback wearConnectorUpdateCallback = new ConnectorUpdateCallback() {
        @Override
        public void onDataUpdated(final Song song, final PlayingState state) {
            if(song == null) return;
            runDataUpdateTask(song, state);
        }

        @Override
        public void onDataUnavailable(final int errorCode, final ConnectionMessage connectionMessage) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.COMM_PATH_UNAVAILABLE);

                    String[] data = new String[3];

                    if (connectionMessage != null) {
                        data[0] = connectionMessage.getTopLine();
                        data[1] = connectionMessage.getMiddleLine();
                        data[2] = connectionMessage.getBottomLine();
                    }

                    dataMap.getDataMap().putStringArray(Constants.COMM_DATA_CONTENT, data);
                    sendDataMap(dataMap);
                }
            }).start();
        }

        @Override
        public void onPlayingStateChanged(final PlayingState state) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.COMM_PATH_STATE);

                    dataMap.getDataMap().putInt(Constants.COMM_DATA_STATE, state.ordinal());

                    sendDataMap(dataMap);
                }
            }).start();
        }
    };

    private void sendDataMap(PutDataMapRequest dataMap) {
        // create request
        PutDataRequest request = dataMap.asPutDataRequest();

        // send data
        DataApi.DataItemResult dataItemResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request).await(5000, TimeUnit.SECONDS);

        Log.e("Sent data map with result: " + dataItemResult.toString());
    }

    public void startAppOnAllConnectedNodes() {
        if (App.isActivityVisible() || App.isNotificationVisible())
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Node> nodes = WearHelper.getNodes(mGoogleApiClient);
                mNodesCounter = nodes.size();
                for (Node node : nodes) {
                    syncStartAppOnNode(node);
                }
            }
        }).start();
    }

    private void asyncStartAppOnNode(final Node node) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                syncStartAppOnNode(node);
            }
        }).start();
    }

    private void syncStartAppOnNode(Node node) {
        MessageApi.SendMessageResult result = sendMessageToNode(node.getId(), Constants.COMM_START);

        if (!result.getStatus().isSuccess()) {
            Log.w("Failed to send start message to node " + node.getDisplayName());
            return;
        }

        Log.d("Wear app started on node: " + node.getDisplayName());
    }

    private MessageApi.SendMessageResult sendMessageToNode(String nodeId, String message) {
        return Wearable.MessageApi.sendMessage(
                mGoogleApiClient, nodeId, message, null).await();
    }

    public void sendMessageAsync(final String message, final MessageResultCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {

                while (!mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting()) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                List<Node> nodes = WearHelper.getNodes(mGoogleApiClient);
                if (nodes.size() == 0) {
                    cancel(true);
                    return false;
                }

                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, nodes.get(0).getId(), message, null).await();

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

    @Override
    public void onConnected(Bundle bundle) {
        // get update callbacks
        App.getInstance().registerConnectorCallback(wearConnectorUpdateCallback);

        new Thread(new Runnable() {
            @Override
            public void run() {
                // get nodes
                mNodesCounter = WearHelper.getNodes(mGoogleApiClient).size();
            }
        }).start();
    }

    @Override
    public void onConnectionSuspended(int i) {
        unregisterCallback();
    }
}
