package com.mv2studio.amarok.kontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

public class Prefs {
	
	public static String ipPortKey = "pref_remote_server",
						 useVolButtonsKey = "pref_volume_change",
						 showNotifyKey = "pref_show_notify",
						 notifyShowPhotoKey = "pref_notify_photo",
						 closePlaylistKey = "pref_close_playlist",
						 notifyUpdateIntervalKey = "pref_notify_interval",
						 updateIntervalKey = "pref_interval",
						 volumeStepKey = "pref_vol_step",
						 clearCacheKey = "pref_clear_cache",
						 use3gKey = "pref_allow_3g",
						 blurKey = "pref_blur";
	
	private static final int defaultPort = 8484;

	private static String ipPort;
	public static boolean useVolButtons, showNotify, notifyShowPhoto, closePlaylist, use3g;
	public static int updateInterval, notifyUpdateInterval, volumeStep, blurIntensity;
	
	private static int screenWidth;

	public static int getScreenWidth() {
		return screenWidth;
	}
	
	public static void setScreenWidth(Context context) {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display d = wm.getDefaultDisplay();
		Point p = new Point();
		d.getSize(p);
		screenWidth = Math.min(p.x, 720);
	}
	
	public static void setIp(SharedPreferences prefs) {
		ipPort = checkUrl(prefs.getString(ipPortKey, ""));
	}
	
	public static String checkUrl(String ip) {
		if(!ip.startsWith("http://")) ip = "http://"+ip;
		boolean useDefaultPort = (ip.split(":").length != 3);
		boolean lastColon = ip.charAt(ip.length()-1) == ':';

		if(useDefaultPort) {
			ip += ":"+defaultPort;
		} else if (lastColon) {
			ip += defaultPort;
		}
		return ip;	
	}
	
	public static String getIp() {
		return ipPort;
	}
	
	public static void setAll(SharedPreferences prefs) {		 
		Prefs.useVolButtons = prefs.getBoolean(useVolButtonsKey, true);
		Prefs.closePlaylist = prefs.getBoolean(closePlaylistKey, false);
		Prefs.updateInterval = Integer.parseInt(prefs.getString(updateIntervalKey, "3"));
		Prefs.volumeStep = Integer.parseInt(prefs.getString(volumeStepKey, "5"));
		Prefs.blurIntensity = Integer.parseInt(prefs.getString(blurKey, "20"));
		setNotificationPrefs(prefs);
	}
	
	public static void setNotificationPrefs(SharedPreferences prefs) {
		Prefs.setIp(prefs);
		Prefs.use3g = prefs.getBoolean(use3gKey, false);
		Prefs.showNotify = prefs.getBoolean(showNotifyKey, true);
		Prefs.notifyShowPhoto = prefs.getBoolean(notifyShowPhotoKey, true);
		Prefs.notifyUpdateInterval = Integer.parseInt(prefs.getString(notifyUpdateIntervalKey, "5"));

	}
	
}
