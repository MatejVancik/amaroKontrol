package com.mv2studio.amarok.kontrol.communication;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.mv2studio.amarok.kontrol.ui.NotificationUtils;
import com.mv2studio.amarok.kontrol.WearApp;
import com.mv2studio.amarok.kontrol.shared.Constants;
import com.mv2studio.amarok.kontrol.shared.Log;
import com.mv2studio.amarok.kontrol.shared.WearHelper;

import java.util.concurrent.TimeUnit;

/**
 * Created by matej on 15.11.14.
 */
public class DataLayerListenerService extends WearableListenerService {

    public static final String UPDATE_SONG_DATA_ACTION = "song_data";
    public static final String UPDATE_UNAVAILABLE_DATA_ACTION = "unavailable_song_data";
    public static final String UPDATE_PLAYBACK_STATE_ACTION = "playback_state";

    public static final String DATA_ARTIST = "artist";
    public static final String DATA_SONG = "album";
    public static final String DATA_COVER_BLURED = "cover_blured";
    public static final String DATA_PLABACK_STATE = "playback";

    private static final int TIMEOUT = 5;

    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = WearApp.getInstance().getGoogleApiClient();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("onDataChanged: " + dataEvents);

        ConnectionResult connectionResult =
                mGoogleApiClient.blockingConnect(TIMEOUT, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e("Failed to connect to GoogleApiClient.");
            return;
        }

        // Loop through the events and send a message
        // to the node that created the data item.
        for (DataEvent event: dataEvents) {
            Log.d("Event received: " + event.getDataItem().getUri());

            String eventUri = event.getDataItem().getUri().toString();

            DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());

            Intent dataIntent = null;

            if (eventUri.contains(Constants.COMM_PATH_SONG)) {
                dataIntent = onDataUpdated(dataItem);
            } else if (eventUri.contains(Constants.COMM_PATH_UNAVAILABLE)) {
                dataIntent = onDataUnavailable(dataItem);
            } else if (eventUri.contains(Constants.COMM_PATH_STATE)) {
                dataIntent = onPlayingStateChanged(dataItem);
            }

            if(dataIntent != null)
                LocalBroadcastManager.getInstance(this).sendBroadcast(dataIntent);
        }
    }

    private Intent onDataUpdated(DataMapItem dataItem) {
        String[] data = dataItem.getDataMap().getStringArray(Constants.COMM_DATA_CONTENT);
        int playbackState = dataItem.getDataMap().getInt(Constants.COMM_DATA_STATE);


        Intent dataIntent = new Intent(UPDATE_SONG_DATA_ACTION);
        dataIntent.putExtra(DATA_ARTIST, data[0]);
        dataIntent.putExtra(DATA_SONG, data[1]);
        dataIntent.putExtra(DATA_PLABACK_STATE, playbackState);

        Bitmap coverArt = null, coverBlured = null;
        if (dataItem.getDataMap().containsKey(Constants.COMM_DATA_COVER)) {
            Asset profileAsset = dataItem.getDataMap().getAsset(Constants.COMM_DATA_COVER);
            coverArt = WearHelper.loadBitmapFromAsset(profileAsset, mGoogleApiClient);
        }

        if (dataItem.getDataMap().containsKey(Constants.COMM_DATA_COVER)) {
            Asset profileAsset = dataItem.getDataMap().getAsset(Constants.COMM_DATA_COVER_BLURED);
            coverBlured = WearHelper.loadBitmapFromAsset(profileAsset, mGoogleApiClient);
            dataIntent.putExtra(DATA_COVER_BLURED, coverBlured);
        }

        NotificationUtils.issueNotification(data[0], data[1], coverArt, coverBlured);

        return dataIntent;
    }

    private Intent onDataUnavailable(DataMapItem dataItem) {
        String[] data = dataItem.getDataMap().getStringArray(Constants.COMM_DATA_CONTENT);

        Intent dataIntent = new Intent(UPDATE_UNAVAILABLE_DATA_ACTION);
        dataIntent.putExtra(DATA_ARTIST, data[0]);
        dataIntent.putExtra(DATA_SONG, data[1]);

        NotificationUtils.issueNotification(data[0], data[1], null, null);

        return dataIntent;
    }

    private Intent onPlayingStateChanged(DataMapItem dataItem) {
        int playbackState = dataItem.getDataMap().getInt(Constants.COMM_DATA_STATE);

        Intent dataIntent = new Intent(UPDATE_PLAYBACK_STATE_ACTION);
        dataIntent.putExtra(DATA_PLABACK_STATE, playbackState);

        return dataIntent;
    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        // new request to start the app
        // all this should probably happen only if notification not up/app is really starting
        if (messageEvent.getPath().equals(Constants.COMM_START)) {
            NotificationUtils.issueNotification("amaroKontrol", "", null, null);
            Commander.getInstance().sendCommand(Constants.COMM_FORCE_REFRESH, null);
        }

        // request to clera notification - app on phone has ended
        else if (messageEvent.getPath().equals(Constants.COMM_CLEAR_NOTIFICATION)) {
            Log.d("clearing notification from wear");
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();

        }
    }


}