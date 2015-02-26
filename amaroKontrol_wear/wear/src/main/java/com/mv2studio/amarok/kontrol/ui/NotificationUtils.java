package com.mv2studio.amarok.kontrol.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.WearApp;
import com.mv2studio.amarok.kontrol.communication.Commander;
import com.mv2studio.amarok.kontrol.communication.DataLayerListenerService;

/**
 * Created by matej on 15.2.15.
 */
public class NotificationUtils {
    private static Context context = WearApp.getInstance();
    private static Intent displayIntent = new Intent(context, ControlActivity.class);

    public static final int NOTIFICATION_ID = 816;

    private static Notification getControlPage() {

        Notification.WearableExtender ext = new Notification.WearableExtender()
                    .setDisplayIntent(PendingIntent.getActivity(context, 0, displayIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT))
                    .setCustomSizePreset(Notification.WearableExtender.SIZE_FULL_SCREEN)
                    .setContentAction(0);

        Notification controlPageNotification = new Notification.Builder(context)
                    .extend(ext)
                    .build();

        return controlPageNotification;
    }


    private static Notification.WearableExtender getExtender() {
        Notification.WearableExtender extender = new Notification.WearableExtender()
                .setContentAction(0)
                .addPage(getControlPage());

        return extender;
    }


    private static Notification.Builder builder;
    public static Notification issueNotification(String artist, String songTitle, Bitmap background, Bitmap blured) {

        // build notification
        if (builder == null) {

            // intent to send play/pause broadcast
            Intent playPauseIntent = new Intent(Commander.COMMAND_PLAY_PAUSE);
            PendingIntent pendingPlayPauseIntent = PendingIntent.getBroadcast(
                    context, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            // create play/pause action button for notification
            Notification.Action action = new Notification.Action.Builder(
                    R.drawable.ic_next, null, pendingPlayPauseIntent).build();

            builder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .addAction(action);
        }

        // update activity/intent accordingly
        displayIntent.putExtra(DataLayerListenerService.DATA_ARTIST, artist);
        displayIntent.putExtra(DataLayerListenerService.DATA_SONG, songTitle);
        displayIntent.putExtra(DataLayerListenerService.DATA_COVER_BLURED, blured);

        builder.setContentTitle(artist)
                .setContentText(songTitle)
                .extend(getExtender()
                        .setBackground(background));

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        return null;
    }


}
