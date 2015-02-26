package com.mv2studio.amarok.kontrol.shared;

/**
 * Created by matej on 16.11.14.
 */
public class Log {

    private static final String TAG = "amaroKontrol";

    public static void w(String message) {
        android.util.Log.w(TAG, message);
    }

    public static void d(String message) {
        android.util.Log.d(TAG, message);
    }

    public static void e(String message) {
        android.util.Log.e(TAG, message);
    }

}
