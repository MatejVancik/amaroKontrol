package com.mv2studio.amarok.kontrol;

import android.app.Application;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by matej on 20.11.14.
 */
public class WearApp extends Application {

    private static WearApp instance;

    public static WearApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    /**
     * Don't forget to call .connect on this!
     * @return
     */
    public GoogleApiClient getGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
    }
}
