package com.mv2studio.amarok.kontrol.communication;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.mv2studio.amarok.kontrol.App;
import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.communication.amarok.AmarokCommand;
import com.mv2studio.amarok.kontrol.shared.Constants;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by matej on 15.11.14.
 */
public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "DataLayerSample";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onDataChanged: " + dataEvents);
        }
        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);

        ConnectionResult connectionResult =
                mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }

        // Loop through the events and send a message
        // to the node that created the data item.
        for (DataEvent event : events) {
            Uri uri = event.getDataItem().getUri();

            // Get the node id from the host value of the URI
            String nodeId = uri.getHost();

            // Set the data of the message to be the bytes of the URI.
            byte[] payload = uri.toString().getBytes();

            // Send the RPC
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId,
                    DATA_ITEM_RECEIVED_PATH, payload);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        String path = messageEvent.getPath();
        if (path.equals(Constants.COMM_START)) {

            // do not start foreground if activity already visible
            if (App.isActivityVisible()) return;

            Intent intent = new Intent(this, ConnectionService.class);
            intent.putExtra(ConnectionService.START_FOREGROUND, true);
            intent.putExtra(ConnectionService.FORCE_UPDATE, true);
            startService(intent);
            return;
        }

        // do not start app, finish all services
        if (!App.isActivityVisible() && !App.isNotificationVisible()) {
            stopService(new Intent(this, ConnectionService.class));
            stopSelf();
            return;
        }

        switch (path) {
            case Constants.WEAR_COMMAND_PLAY:
                AmarokCommand.PLAY_PAUSE.execute();
                break;

            case Constants.WEAR_COMMAND_NEXT:
                AmarokCommand.NEXT.execute();
                break;

            case Constants.WEAR_COMMAND_PREV:
                AmarokCommand.PREV.execute();
                break;

            case Constants.WEAR_COMMAND_VOL_DOWN:
                AmarokCommand.VOL_DOWN.param(Prefs.volumeStep + "").execute();
                break;

            case Constants.WEAR_COMMAND_VOL_UP:
                AmarokCommand.VOL_UP.param(Prefs.volumeStep + "").execute();
                break;

            case Constants.COMM_FORCE_REFRESH:
                App.getInstance().sendBroadcast(new Intent(ConnectionService.FORCE_UPDATE));
                break;
        }

    }
}