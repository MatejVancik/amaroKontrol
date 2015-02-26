package com.mv2studio.amarok.kontrol.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.mv2studio.amarok.kontrol.App;

/**
 * Created by matej on 26.2.15.
 */
public class Storage {

    public static boolean getBoolValue(String key) {
        SharedPreferences prefs = getDataPrefs();
        return prefs.getBoolean(key, false);
    }

    public static void storeBoolValue(String key, boolean value) {
        SharedPreferences prefs = getDataPrefs();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private static SharedPreferences getDataPrefs() {
        return App.getInstance().getSharedPreferences("PREFS", Context.MODE_PRIVATE);
    }

}
