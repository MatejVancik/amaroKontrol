//package com.mv2studio.amarok.kontrol.notification;
//
//import android.annotation.SuppressLint;
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Build.VERSION;
//import android.support.v4.app.NotificationCompat;
//import android.widget.RemoteViews;
//
//import com.mv2studio.amarok.kontrol.Prefs;
//import com.mv2studio.amarok.kontrol.R;
//import com.mv2studio.amarok.kontrol.helpers.FileHelper;
//import com.mv2studio.amarok.kontrol.shared.model.Song;
//import com.mv2studio.amarok.kontrol.notification.NotificationService.PlayingState;
//import com.mv2studio.amarok.kontrol.ui.Activity.BaseActivity;
//
//import java.io.File;
//
//public class AmarokNotification {
//
//    NotificationService mContext;
//    private static final int NOTIFICATION_ID = 1;
//    private RemoteViews mExtendView;
//    private RemoteViews mSmallView;
//    private Notification mNotification;
//    private Song mCurrentSong = new Song("", "", "");
//    private boolean isForegroundEnabled;
//    private boolean mBuilt = false;
//
//    public AmarokNotification(Service service) {
//        this.mContext = (NotificationService) service;
//
//        mCurrentSong = NotificationService.currentSong;
//    }
//
//    @SuppressLint("NewApi")
//    private void prepareViews() {
//
//        // set layouts
//        int extendViewID = (Prefs.notifyShowPhoto) ? R.layout.notification : R.layout.notification_expanded;
//        mExtendView = new RemoteViews(mContext.getPackageName(), extendViewID);
//        mSmallView = new RemoteViews(mContext.getPackageName(), R.layout.notification_small);
//
//        if(!mCurrentSong.equals(new Song("", "", ""))) {
//            mExtendView.setTextViewText(R.id.notifyArtist, mCurrentSong.getArtist());
//            mExtendView.setTextViewText(R.id.notifyTitle, mCurrentSong.getTitle());
//            mExtendView.setTextViewText(R.id.notifyAlbum, mCurrentSong.getAlbum());
//
//            mSmallView.setTextViewText(R.id.notifyArtist, mCurrentSong.getArtist());
//            mSmallView.setTextViewText(R.id.notifyTitle, mCurrentSong.getTitle());
//        } else {
//            mExtendView.setTextViewText(R.id.notifyArtist, mContext.getResources().getString(R.string.not_playing));
//            mExtendView.setTextViewText(R.id.notifyTitle, mContext.getResources().getString(R.string.app_name));
//            mExtendView.setTextViewText(R.id.notifyAlbum, "");
//
//            mSmallView.setTextViewText(R.id.notifyArtist, mContext.getResources().getString(R.string.not_playing));
//            mSmallView.setTextViewText(R.id.notifyTitle, mContext.getResources().getString(R.string.app_name));
//        }
//
//
//
//
//        Intent nextIntent = new Intent("next");
//        nextIntent.setAction("next");
//        PendingIntent pendingNext = PendingIntent.getBroadcast(mContext, 0, nextIntent, 0);
//        mExtendView.setOnClickPendingIntent(R.id.notifyNext, pendingNext);
//        mSmallView.setOnClickPendingIntent(R.id.notifyNext, pendingNext);
//
//        Intent prevIntent = new Intent("prev");
//        prevIntent.setAction("prev");
//        PendingIntent pendingPrev = PendingIntent.getBroadcast(mContext, 0, prevIntent, 0);
//        mExtendView.setOnClickPendingIntent(R.id.notifyPrev, pendingPrev);
//
//        Intent playIntent = new Intent("play");
//        playIntent.setAction("play");
//        PendingIntent pendingPlay = PendingIntent.getBroadcast(mContext, 0, playIntent, 0);
//        mExtendView.setOnClickPendingIntent(R.id.notifyPlay, pendingPlay);
//        mSmallView.setOnClickPendingIntent(R.id.notifyPlay, pendingPlay);
//
//
//        Intent exitIntent = new Intent("exit");
//        exitIntent.setAction("exit");
//        PendingIntent pendingExit = PendingIntent.getBroadcast(mContext, 0, exitIntent, 0);
//        mExtendView.setOnClickPendingIntent(R.id.notifyExit, pendingExit);
//        mSmallView.setOnClickPendingIntent(R.id.notifyExit, pendingExit);
//
//        // bind views to notification
//        mNotification.contentView = mSmallView;
//        if(VERSION.SDK_INT >= 16) mNotification.bigContentView = mExtendView;
//
//        mBuilt = true;
//    }
//
//    public void updateNotification() {
//        // no notification at all
//        if(!isForegroundEnabled) return;
//
//        // update cover only if it's different album
//        if(!mCurrentSong.onSameAlbum(NotificationService.currentSong) && FileHelper.isExternalStorageReadable()) {
//            // TODO: NULL POINTER z Crashes & ANR
//            String path = mContext.getExternalCacheDir().toString() +"/"+ FileHelper.getSafeString(NotificationService.currentSong.getArtist()+NotificationService.currentSong.getAlbum())+"_mini.jpg";
//            File file = new File(path);
//            if(file.exists()) {
//                Uri uri = Uri.fromFile(file);
//                mSmallView.setImageViewUri(R.id.notifyCover, uri);
//                mExtendView.setImageViewUri(R.id.notifyCover, uri);
//            } else {
//                mSmallView.setImageViewResource(R.id.notifyCover, R.drawable.ic_launcher);
//                mExtendView.setImageViewResource(R.id.notifyCover, R.drawable.ic_launcher);
//            }
//        }
//
//        // update artist photo only if it's different artist
//        if(!mCurrentSong.getArtist().equals(NotificationService.currentSong.getArtist()) && FileHelper.isExternalStorageReadable()) {
//            String path = mContext.getExternalCacheDir().toString() + File.separator + FileHelper.getSafeString(NotificationService.currentSong.getArtist()) + ".jpg";
//            File file = new File(path);
//            if(file.exists()) {
//                Uri uri = Uri.fromFile(file);
//                mExtendView.setImageViewUri(R.id.notifyPhoto, uri);
//            } else {
//                mExtendView.setImageViewResource(R.id.notifyPhoto, R.drawable.no_photo);
//            }
//        }
//
//
//
//        mCurrentSong = NotificationService.currentSong;
//        mExtendView.setTextViewText(R.id.notifyArtist, mCurrentSong.getArtist());
//        mExtendView.setTextViewText(R.id.notifyTitle, mCurrentSong.getTitle());
//        mExtendView.setTextViewText(R.id.notifyAlbum, mCurrentSong.getAlbum());
//
//        mSmallView.setTextViewText(R.id.notifyArtist, mCurrentSong.getArtist());
//        mSmallView.setTextViewText(R.id.notifyTitle, mCurrentSong.getTitle());
//
//        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
//        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
//    }
//
//
//    private void buildNotification() {
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
//                .setSmallIcon(R.drawable.ic_stat_notify)
//                .setContentTitle((mCurrentSong != null) ? mCurrentSong.getTitle() : mContext.getResources().getString(R.string.app_name))
//                .setContentText((mCurrentSong != null) ? mCurrentSong.getArtist() : mContext.getResources().getString(R.string.not_playing));
//
//        Intent intent = new Intent(mContext, BaseActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, 0, intent, Notification.FLAG_ONGOING_EVENT);
//
//        builder.setContentIntent(resultPendingIntent);
//        mNotification = builder.build();
//        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
//    }
//
//    public void startForeground() {
//        if(!mBuilt) {
//            buildNotification();
//            prepareViews();
//        }
//        isForegroundEnabled = true;
//        updateNotification();
//        setPlayPauseButton();
//        mContext.startForeground(NOTIFICATION_ID, mNotification);
//    }
//
//    public void stopForeground() {
//        isForegroundEnabled = false;
//        mContext.stopForeground(true);
//    }
//
//    public boolean isForeground() {
//        return isForegroundEnabled;
//    }
//
//
//    public void setPlayPauseButton() {
//        // no notification at all
//        if(!isForegroundEnabled) return;
//
//        int img = (PlayingState.state == PlayingState.PLAYING) ? R.drawable.pause : R.drawable.play;
//        mSmallView.setImageViewResource(R.id.notifyPlay, img);
//        mExtendView.setImageViewResource(R.id.notifyPlay, img);
//
//        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
//        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
//    }
//
//}
