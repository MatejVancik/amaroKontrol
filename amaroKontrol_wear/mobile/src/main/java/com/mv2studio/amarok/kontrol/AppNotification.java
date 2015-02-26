package com.mv2studio.amarok.kontrol;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.mv2studio.amarok.kontrol.communication.ConnectionMessage;
import com.mv2studio.amarok.kontrol.communication.ConnectorUpdateCallback;
import com.mv2studio.amarok.kontrol.helpers.FileHelper;
import com.mv2studio.amarok.kontrol.shared.PlayingState;
import com.mv2studio.amarok.kontrol.shared.model.Song;
import com.mv2studio.amarok.kontrol.ui.Activity.BaseActivity;

import java.io.File;

/**
 * Created by matej on 17.11.14.
 */
public class AppNotification {


    public static final String ACTION_PLAY = App.PCKG_NAME + "PLAY";
    public static final String ACTION_PREV = App.PCKG_NAME + "PREV";
    public static final String ACTION_NEXT = App.PCKG_NAME + "NEXT";
    public static final String ACTION_EXIT = App.PCKG_NAME + "EXIT";

    public static final int NOTIFICATION_ID = 1;


    private Context mContext;
    private RemoteViews mExtendView;
    private RemoteViews mSmallView;

    private Notification mNotification;
    private boolean isForegroundEnabled;


    private Song mCurrentSong;
    private PlayingState mCurrentState;

    public AppNotification() {
        mContext = App.getInstance();
        App.getInstance().registerConnectorCallback(mConnectorUpdateCallbacks);

        buildNotification();
        prepareViews();
    }

    public void unregisterCallbacks() {
        App.getInstance().unregisterConnectorCallback(mConnectorUpdateCallbacks);
    }

    private ConnectorUpdateCallback mConnectorUpdateCallbacks = new ConnectorUpdateCallback() {
        @Override
        public void onDataUpdated(Song song, PlayingState state) {
            // update notification
            if (mCurrentSong == null || !mCurrentSong.equals(song)) {
                updateNotification(song);
                mCurrentSong = song;
                mCurrentState = state;
            }
        }

        @Override
        public void onDataUnavailable(int errorCode, ConnectionMessage connectionMessage) {
            String titleData = "", artistData = "", albumData = "";

            if (connectionMessage != null) {
                titleData = connectionMessage.getTopLine();
                artistData = connectionMessage.getMiddleLine();
                albumData = connectionMessage.getBottomLine();
            }

            updateNotification(new Song(titleData, artistData, albumData));
        }

        @Override
        public void onPlayingStateChanged(PlayingState state) {
            setPlayPauseButton(state);
            mCurrentState = state;
        }
    };



    @SuppressLint("NewApi")
    private void prepareViews() {

        // set layouts
        int extendViewID = (Prefs.notifyShowPhoto) ? R.layout.notification_photo : R.layout.notification_expanded;
        mExtendView = new RemoteViews(mContext.getPackageName(), extendViewID);
        mSmallView = new RemoteViews(mContext.getPackageName(), R.layout.notification_small);

        Intent nextIntent = new Intent(ACTION_NEXT);
        PendingIntent pendingNext = PendingIntent.getBroadcast(mContext, 0, nextIntent, 0);
        mExtendView.setOnClickPendingIntent(R.id.notifyNext, pendingNext);
        mSmallView.setOnClickPendingIntent(R.id.notifyNext, pendingNext);

        Intent prevIntent = new Intent(ACTION_PREV);
        PendingIntent pendingPrev = PendingIntent.getBroadcast(mContext, 0, prevIntent, 0);
        mExtendView.setOnClickPendingIntent(R.id.notifyPrev, pendingPrev);

        Intent playIntent = new Intent(ACTION_PLAY);
        PendingIntent pendingPlay = PendingIntent.getBroadcast(mContext, 0, playIntent, 0);
        mExtendView.setOnClickPendingIntent(R.id.notifyPlay, pendingPlay);
        mSmallView.setOnClickPendingIntent(R.id.notifyPlay, pendingPlay);

        Intent exitIntent = new Intent(ACTION_EXIT);
        PendingIntent pendingExit = PendingIntent.getBroadcast(mContext, 0, exitIntent, 0);
        mExtendView.setOnClickPendingIntent(R.id.notifyExit, pendingExit);
        mSmallView.setOnClickPendingIntent(R.id.notifyExit, pendingExit);

        // bind views to notification
        mNotification.contentView = mSmallView;
        if(Build.VERSION.SDK_INT >= 16) mNotification.bigContentView = mExtendView;
    }

    private void buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_stat_notify);
//                .setContentTitle((mCurrentSong != null) ? mCurrentSong.getTitle() : mContext.getResources().getString(R.string.app_name))
//                .setContentText((mCurrentSong != null) ? mCurrentSong.getArtist() : mContext.getResources().getString(R.string.not_playing));

        Intent intent = new Intent(mContext, BaseActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

        builder.setContentIntent(resultPendingIntent);
        mNotification = builder.build();
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
    }

    public void updateNotification(Song newSong) {

        // no notification to update
        if(!isForegroundEnabled) return;

        if(FileHelper.isExternalStorageReadable()) {

            // UPDATE COVER
            String path = "";
            if(mContext.getExternalCacheDir() != null)
                path = mContext.getExternalCacheDir().toString() + "/" +
                       FileHelper.getSafeString(newSong.getArtist()
                       + newSong.getAlbum()) + "_mini.jpg";

            File file = new File(path);
            if(file.exists()) {
                Uri uri = Uri.fromFile(file);
                mSmallView.setImageViewUri(R.id.notifyCover, uri);
                mExtendView.setImageViewUri(R.id.notifyCover, uri);
            } else {
                mSmallView.setImageViewResource(R.id.notifyCover, R.drawable.ic_launcher);
                mExtendView.setImageViewResource(R.id.notifyCover, R.drawable.ic_launcher);
            }

            // UPDATE PHOTO
            path = "";
            if (mContext.getExternalCacheDir() != null) {
                path = mContext.getExternalCacheDir().toString() + File.separator
                        + FileHelper.getSafeString(newSong.getArtist()) + ".jpg";
            }

            file = new File(path);
            if (file.exists()) {
                Uri uri = Uri.fromFile(file);
                mExtendView.setImageViewUri(R.id.notifyPhoto, uri);
            } else {
                mExtendView.setImageViewResource(R.id.notifyPhoto, R.drawable.no_photo);
            }
        }

        mExtendView.setTextViewText(R.id.notifyArtist, newSong.getArtist());
        mExtendView.setTextViewText(R.id.notifyTitle, newSong.getTitle());
        mExtendView.setTextViewText(R.id.notifyAlbum, newSong.getAlbum());

        mSmallView.setTextViewText(R.id.notifyArtist, newSong.getArtist());
        mSmallView.setTextViewText(R.id.notifyTitle, newSong.getTitle());

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    /**
     * Update notification content with current song and get system notification
     * @return
     */
    public Notification prepareForeground() {
        isForegroundEnabled = true;
        if(mCurrentSong != null) updateNotification(mCurrentSong);
        setPlayPauseButton(mCurrentState);
        return mNotification;
    }

    public void foregroundStoped() {
        isForegroundEnabled = false;
    }

    public void setPlayPauseButton(PlayingState state) {
        // no notification to update
        if(!isForegroundEnabled) return;

        int img = (state == PlayingState.PLAYING) ? R.drawable.pause : R.drawable.play;
        mSmallView.setImageViewResource(R.id.notifyPlay, img);
        mExtendView.setImageViewResource(R.id.notifyPlay, img);

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

}
