package com.mv2studio.amarok.kontrol.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Environment;

import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.shared.model.Song;
import com.mv2studio.amarok.kontrol.ui.Activity.BaseActivity;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class MediaHelper {


    public static Bitmap downloadBitmap(String url, String cacheTag, Context context) {
        return downloadBitmap(url, cacheTag, context, false);
    }

    public static Bitmap downloadBitmap(String url, String cacheTag, Context context, boolean createMini) {
        cacheTag = FileHelper.getSafeString(cacheTag);

        Bitmap bitmap;
        bitmap = readFromCache(cacheTag, context);
        if (bitmap != null) return bitmap;

        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                return null;
            }
            InputStream inputStream = null;
            try {
                inputStream = response.body().byteStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null && (bitmap = readFromCache(cacheTag, context)) != null)
                    return bitmap;
                if (cacheTag != null && bitmap != null)
                    saveToCache(bitmap, cacheTag, context, createMini);
                return bitmap;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getBluredBitmap(String cacheTag, Bitmap bitmapToBlur, Context context) {
        if (bitmapToBlur == null)
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.albumblured);

        cacheTag = FileHelper.getSafeString(cacheTag);
        Bitmap bitmap = readFromCache(cacheTag + "blured", context);

        if (bitmap != null)
            return bitmap;

        bitmap = BitmapHelper.fastblur(bitmapToBlur, Prefs.blurIntensity);
        if(FileHelper.isExternalStorageWritable()) {
            File file = new File(context.getExternalCacheDir().toString() + "/"+cacheTag+"blured.jpg");
            if(!file.exists())
                saveToCache(bitmap, cacheTag + "blured", context, false);
        }
        return bitmap;
    }

    private static void saveToCache(Bitmap cover, String cacheTag, Context context, boolean createMini) {
        if(!FileHelper.isExternalStorageWritable()) return;
        cacheTag = FileHelper.getSafeString(cacheTag);

        FileOutputStream fo;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            cover.compress(Bitmap.CompressFormat.PNG, 0, bytes);
            context.getExternalCacheDir().mkdirs();

            File f = new File(context.getExternalCacheDir().toString() + File.separator + cacheTag + ".jpg");
            f.createNewFile();

            fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            fo.close();

            if(createMini) {
                f = new File(context.getExternalCacheDir().toString() + File.separator + cacheTag + "_mini.jpg");
                if(f.exists()) return;
                f.createNewFile();
                bytes = new ByteArrayOutputStream();
                cover = BitmapHelper.scaleToFitWidth(cover, BitmapHelper.convertDPtoPX(128, context));
                cover.compress(Bitmap.CompressFormat.PNG, 0, bytes);

                fo = new FileOutputStream(f);
                fo.write(bytes.toByteArray());
                fo.close();
            }

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    public static Bitmap readFromCache(String cacheTag, Context context) {
        if(!FileHelper.isExternalStorageReadable()) return null;
        cacheTag = FileHelper.getSafeString(cacheTag);
        try {
            File file = new File(context.getExternalCacheDir().toString() + File.separator + cacheTag + ".jpg");
            FileInputStream fis = new FileInputStream(file);

            Bitmap cover = BitmapFactory.decodeStream(fis);
            fis.close();
            return cover;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } catch (NullPointerException ex) {
        }
        return null;
    }

    public static void downloadSong(final int pos, final Song song, final BaseActivity activity) {

    new AsyncTask<Void, Integer, Void>(){

        protected void onPreExecute() {
            activity.setActionBarInfoCancelTask(this);
            activity.showActionBarInfo(this);
            activity.setActionBarInfoText(activity.getString(R.string.navbar_downloading_song)+"0%", this);
        }

        protected void onPostExecute(Void result) {
            activity.hideActionBarInfo(this);
        }

        @Override
        protected Void doInBackground(Void... param) {
            int count;
            try {
                URL url = new URL(Prefs.getIp()+"/getSongFile/"+pos);
                URLConnection conexion = url.openConnection();
                conexion.connect();
                // this will be useful so that you can show a tipical 0-100% progress bar
                int lenghtOfFile = conexion.getContentLength();
                String mimeType = conexion.getContentType();
                // downlod the file
                InputStream input = new BufferedInputStream(url.openStream());

                String savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Environment.DIRECTORY_MUSIC + File.separator ;
                savePath += song.getArtist() + File.separator + song.getAlbum();

                new File(savePath).mkdirs();

                OutputStream output = new FileOutputStream(savePath + File.separator + song.getTitle() + "." + mimeType);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    publishProgress((int)(total*100/lenghtOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
                new MediaScannerWrapper(savePath, activity).scan();
            } catch (Exception e) {
            }
            return null;
        }

        int prev = 0;
        protected void onProgressUpdate(Integer[] values) {
            if(prev < values[0]) {
                prev = values[0];
                activity.setActionBarInfoText(activity.getString(R.string.navbar_downloading_song)+values[0]+"%", this);
            }
        }

    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    private static class MediaScannerWrapper implements MediaScannerConnection.MediaScannerConnectionClient{
        MediaScannerConnection connection;
        String path;
        public MediaScannerWrapper(String path, Context context) {
            connection = new MediaScannerConnection(context, this);
            this.path = path;
        }
        public void scan() {connection.connect();}
        @Override public void onMediaScannerConnected() { connection.scanFile(path, null); }
        @Override public void onScanCompleted(String path, Uri uri) { connection.disconnect(); }
    }
}
