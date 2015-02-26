package com.mv2studio.amarok.kontrol;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.mv2studio.amarok.kontrol.communication.Command;
import com.mv2studio.amarok.kontrol.communication.ConnectionMessage;
import com.mv2studio.amarok.kontrol.communication.ConnectionService;
import com.mv2studio.amarok.kontrol.communication.Connector;
import com.mv2studio.amarok.kontrol.communication.ConnectorUpdateCallback;
import com.mv2studio.amarok.kontrol.communication.amarok.AmarokConnector;
import com.mv2studio.amarok.kontrol.communication.amarok.PlaylistUpdateCallback;
import com.mv2studio.amarok.kontrol.helpers.CommonHelper;
import com.mv2studio.amarok.kontrol.shared.PlayingState;
import com.mv2studio.amarok.kontrol.shared.model.Song;

import java.util.HashSet;
import java.util.Set;

public class App extends Application {

    public static final String PCKG_NAME = "com.mv2studio.amarok.kontrol.";

	private static boolean sActivityVisible;
    private static boolean sNotificationVisible;
    private static App sContext;

    private Connector activeConnector;

    private Set<ConnectorUpdateCallback> connectorUpdateCallbackList = new HashSet<ConnectorUpdateCallback>();

	public App() {
		sContext = this;
	}

	@Override
	public void onCreate() {
        super.onCreate();
//		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());
//		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());

		// getPrefs
		PreferenceManager.setDefaultValues(App.this, R.xml.preferences, false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.this);
		Prefs.setAll(prefs);
		Prefs.setScreenWidth(App.this);

        activeConnector = new AmarokConnector(this);
        activeConnector.setConnectorCallback(activeConnectorUpdateCallback);

        Intent intent = new Intent(this, ConnectionService.class);
        startService(intent);
	}

    public static App getInstance() {
        return sContext;
    }

	public static void activityResumed() {
		sActivityVisible = true;
		sContext.sendBroadcast(new Intent(ConnectionService.STOP_FOREGROUND));
        if (!CommonHelper.isServiceRunning(ConnectionService.class)) {
            Intent intent = new Intent(getInstance(), ConnectionService.class);
            getInstance().startService(intent);
        }
    }

	public static void activityPaused() {
		sActivityVisible = false;
		Handler h = new Handler();
		h.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!sActivityVisible)
					sContext.sendBroadcast(new Intent(ConnectionService.START_FOREGROUND));
			}
		}, 100);
	}

	public static boolean isActivityVisible() {
		return sActivityVisible;
	}

    public static void setNotificationVisibility(boolean isVisible) {
        sNotificationVisible = isVisible;
    }

    public static boolean isNotificationVisible() {
        return sNotificationVisible;
    }

    private ConnectorUpdateCallback activeConnectorUpdateCallback = new ConnectorUpdateCallback() {
        @Override
        public void onDataUpdated(Song song, PlayingState state) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(ConnectorUpdateCallback.BUNDLE_SONG, song);
            bundle.putInt(ConnectorUpdateCallback.BUNDLE_STATE, state.ordinal());

            for (ConnectorUpdateCallback clbk : connectorUpdateCallbackList) {
                clbk.send(RC_DATA_UPDATED, bundle);
            }
        }

        @Override
        public void onDataUnavailable(int errorCode, ConnectionMessage connectionMessage) {
            Bundle bundle = new Bundle();
            bundle.putInt(BUNDLE_ERROR_CODE, errorCode);
            bundle.putParcelable(BUNDLE_CONNECTION_MESSAGE, connectionMessage);

            for (ConnectorUpdateCallback clbk : connectorUpdateCallbackList) {
                clbk.send(RC_DATA_UNAVAILABLE, bundle);
            }
        }

        @Override
        public void onPlayingStateChanged(PlayingState state) {
            Bundle bundle = new Bundle();
            bundle.putInt(ConnectorUpdateCallback.BUNDLE_STATE, state.ordinal());

            for (ConnectorUpdateCallback clbk : connectorUpdateCallbackList) {
                clbk.send(RC_PLAYING_STATE_CHANGED, bundle);
            }
        }
    };

    public void updatePlaylist(PlaylistUpdateCallback callback) {
        activeConnector.updatePlaylist(callback);
    }

    public void registerConnectorCallback(ConnectorUpdateCallback callback) {
        connectorUpdateCallbackList.add(callback);
    }

    public void unregisterConnectorCallback(ConnectorUpdateCallback callback) {
        connectorUpdateCallbackList.remove(callback);
    }

    public void sendCommand(Command command) {
        activeConnector.sendCommand(command);
    }

    public Connector getActiveConnector() {
        return activeConnector;
    }
}
