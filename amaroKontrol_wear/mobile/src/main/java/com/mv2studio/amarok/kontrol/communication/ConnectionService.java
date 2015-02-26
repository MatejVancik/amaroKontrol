package com.mv2studio.amarok.kontrol.communication;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.mv2studio.amarok.kontrol.App;
import com.mv2studio.amarok.kontrol.AppNotification;
import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.communication.amarok.AmarokCommand;
import com.mv2studio.amarok.kontrol.helpers.CommonHelper;
import com.mv2studio.amarok.kontrol.shared.Constants;
import com.mv2studio.amarok.kontrol.shared.Log;
import com.mv2studio.amarok.kontrol.shared.PlayingState;

/**
 * Created by matej on 17.11.14.
 */
public class ConnectionService extends Service {

    public static final String START_FOREGROUND = App.PCKG_NAME + "START_FOREGROUND";
    public static final String STOP_FOREGROUND = App.PCKG_NAME + "STOP_FOREGROUND";
    public static final String FORCE_UPDATE = App.PCKG_NAME + "FORCE_UPDATE";

    private Handler mHandler;
    private WifiManager.WifiLock lock;

    private Connector mConnector;
    private WearHandler mWearHandler;

    private AppNotification mNotification;
    private NotificationActionReceiver mReceiver;

    private boolean isForeground;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent.getBooleanExtra(START_FOREGROUND, false) && !App.isActivityVisible()) {
            Log.e("Starting foreground from intent");
            startForeground();
        }

        if (intent.getBooleanExtra(FORCE_UPDATE, false)) {
            Log.e("Updating wear from ConnectionService.onStartcommand for instance "+mWearHandler.toString());
            mWearHandler.updateWatch();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mConnector = App.getInstance().getActiveConnector();

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Lock");
        lock.acquire();

        mNotification = new AppNotification();
        mWearHandler = new WearHandler(this, mConnector);

        mHandler = new Handler();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppNotification.ACTION_EXIT);
        intentFilter.addAction(AppNotification.ACTION_NEXT);
        intentFilter.addAction(AppNotification.ACTION_PLAY);
        intentFilter.addAction(AppNotification.ACTION_PREV);
        intentFilter.addAction(START_FOREGROUND);
        intentFilter.addAction(STOP_FOREGROUND);
        intentFilter.addAction(FORCE_UPDATE);

        mReceiver = new NotificationActionReceiver();
        registerReceiver(mReceiver, intentFilter);

        new Thread(mMainLoopTask).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground();
        mWearHandler.unregisterCallback();
        mNotification.unregisterCallbacks();
        mHandler.removeCallbacks(mMainLoopTask);
        lock.release();
        unregisterReceiver(mReceiver);
    }

    public void startForeground() {
        isForeground = true;
        App.setNotificationVisibility(isForeground);
        startForeground(AppNotification.NOTIFICATION_ID, mNotification.prepareForeground());
    }

    public void stopForeground() {
        isForeground = false;
        App.setNotificationVisibility(isForeground);
        mNotification.foregroundStoped();
        stopForeground(true);
    }

    private Runnable mMainLoopTask = new Runnable() {
        @Override
        public void run() {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (!pm.isScreenOn() && mWearHandler.getNodesCount() == 0) {
                mHandler.postDelayed(this, isForeground ? Prefs.notifyUpdateInterval*1000 : Prefs.updateInterval*1000);
                return;
            }

            // set Prefs
            Prefs.setNotificationPrefs(PreferenceManager.getDefaultSharedPreferences(ConnectionService.this));
            Prefs.setScreenWidth(ConnectionService.this);

            // check the global background data setting
            if(!CommonHelper.isWifiConnected(ConnectionService.this) && !Prefs.use3g && isForeground) {
                // do not forget to clear notification from wear
                mWearHandler.sendMessageAsync(Constants.COMM_CLEAR_NOTIFICATION, null);

                // disable service if lost connection in foreground
                stopForeground();
                stopSelf();

                // send info
                mConnector.notifyConnectionUnavailable();
            } else {

                // run update task
                new Thread(mConnector.getUpdateTask()).start();
            }

            mHandler.postDelayed(mMainLoopTask, isForeground ?
                    Prefs.notifyUpdateInterval * 1000 : Prefs.updateInterval * 1000);

        }
    };

    private Runnable mUpdateWearData = new Runnable() {
        @Override
        public void run() {

        }
    };


    public class NotificationActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(AppNotification.ACTION_EXIT)) {
                // remove notification from wear and stop service
                mWearHandler.sendMessageAsync(Constants.COMM_CLEAR_NOTIFICATION, null);

                stopSelf();
            } else if (action.equals(AppNotification.ACTION_NEXT)) {
                mConnector.sendCommand(AmarokCommand.NEXT);
            } else if (action.equals(AppNotification.ACTION_PLAY)) {
                mConnector.sendCommand(AmarokCommand.PLAY_PAUSE);
            } else if (action.equals(AppNotification.ACTION_PREV)) {
                mConnector.sendCommand(AmarokCommand.PREV);
            } else if (action.equals(START_FOREGROUND)) {
                startForeground();
            } else if (action.equals(STOP_FOREGROUND)) {
                stopForeground();
            } else if (action.equals(FORCE_UPDATE)) {
                Log.e("FORCE UPDATE RECEIVED MOBILE");
                mWearHandler.updateWatch();
            }
        }
    }
}
