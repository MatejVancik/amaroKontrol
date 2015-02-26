package com.mv2studio.amarok.kontrol.shared;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by matej on 16.11.14.
 */
public class WearHelper {

    private static final int TIMEOUT = 5;

    public static Bitmap loadBitmapFromAsset(Asset asset, GoogleApiClient googleApiClient) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        ConnectionResult result =
                googleApiClient.blockingConnect(TIMEOUT, TimeUnit.SECONDS);

        if (!result.isSuccess()) {
            return null;
        }

        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                googleApiClient, asset).await().getInputStream();
        googleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w("Requested an unknown Asset.");
            return null;
        }

        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

    public static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    public static List<Node> getNodes(GoogleApiClient googleApiClient) {
        List<Node> results = new ArrayList<Node>();

        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node);
            android.util.Log.d("", "Node found: " + node.getDisplayName());
        }
        return results;
    }

}
