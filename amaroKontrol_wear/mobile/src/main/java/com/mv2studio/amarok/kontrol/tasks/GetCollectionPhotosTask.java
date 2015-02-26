package com.mv2studio.amarok.kontrol.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.AsyncTask;

import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.helpers.BitmapHelper;
import com.mv2studio.amarok.kontrol.helpers.CommonHelper;
import com.mv2studio.amarok.kontrol.helpers.FileHelper;
import com.mv2studio.amarok.kontrol.photosbrowser.LastFmParser;
import com.mv2studio.amarok.kontrol.shared.model.Artist;
import com.mv2studio.amarok.kontrol.ui.Activity.BaseActivity;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetCollectionPhotosTask extends AsyncTask<Void, Integer, Void> {

	private ArrayList<Artist> mArtistsList;
    private BaseActivity mBaseActivity;

    // this should run only once per application start
	private static boolean done = false;
    private static boolean running = false;
	
	public GetCollectionPhotosTask(ArrayList<Artist> artistsList, BaseActivity baseActivity) {
		if(done) return;
		this.mArtistsList = artistsList;
		this.mBaseActivity = baseActivity;
		this.mBaseActivity.setActionBarInfoCancelTask(this);
	}
	
	@Override
	protected void onPreExecute() {
		if (done) return;
		mBaseActivity.showActionBarInfo(this);
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		if(done || !FileHelper.isExternalStorageWritable()) {
			if(!running) cancel(true);
			return null;
		}

		File extPath = mBaseActivity.getExternalCacheDir();
		
		if(!CommonHelper.isOnline(mBaseActivity)) { return null;}
		done = true;
		running = true;
		int progress = 0;

        List<String> cachedFiles = Arrays.asList(extPath.list());

		
		for(Artist artist: mArtistsList) {
			if(isCancelled()) return null;

            String safeArtistName = FileHelper.getSafeString(artist.getName());

			if(cachedFiles.contains(safeArtistName+ ".jpg"))
            { publishProgress(++progress); continue; }
			
			LastFmParser parser  = new LastFmParser(artist.getName(), mBaseActivity);
			InputStream is;
			try {

                // get link for artist photo
				String url = parser.getLink();
				if(url == null || url.isEmpty()) { publishProgress(++progress); continue; }

				// get image from url
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();

                Response response = client.newCall(request).execute();

                // not OK? next image please
		        if (!response.isSuccessful()) continue;
		        
		        if (response.body().byteStream() != null) {
                    is = response.body().byteStream();

                    // create image file
                    File f = new File(extPath.toString() + File.separator + safeArtistName + ".jpg");
                    f.createNewFile();

                    // read image
                    FileOutputStream fos = new FileOutputStream(f);
                    Bitmap decoded = BitmapFactory.decodeStream(is);
                    try {
                        // adjust photo to not cut faces
                        Bitmap photo = cutFaces(BitmapHelper.scaleToFitWidth(decoded, Prefs.getScreenWidth()));
                        photo.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    } catch (NullPointerException ex) {
                        ex.printStackTrace();
                    }

                    // now create thumbnail of photo
                    if (decoded != null) {
                        f = new File(extPath.toString() + File.separator + safeArtistName + "_mini.jpg");
                        f.createNewFile();
                        fos = new FileOutputStream(f);
                        decoded = BitmapHelper.scaleToFitWidth(decoded, BitmapHelper.convertDPtoPX(80, mBaseActivity));
                        decoded.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    }
                    fos.close();
                }
            } catch (IndexOutOfBoundsException | IOException ex) {
                ex.printStackTrace();
			} catch (CommunicationException ex) {			
				publishProgress(-1);
				try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
				running = false;
				return null;
			}
            publishProgress(++progress);
		}	
		running = false;
		return null;
	}
	
	@Override
	protected void onCancelled() {
		running = false;
		onPostExecute(null);
	}

	private Bitmap convert(Bitmap bitmap, Bitmap.Config config) {
	    Bitmap convertedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config);
	    Canvas canvas = new Canvas(convertedBitmap);
	    Paint paint = new Paint();
	    paint.setColor(Color.BLACK);
	    canvas.drawBitmap(bitmap, 0, 0, paint);
	    return convertedBitmap;
	}
	
	
	private Bitmap cutFaces(Bitmap photo) {
		if (photo == null)
			return null;
		else {
			try {
				photo = convert(photo, Bitmap.Config.RGB_565);
				Bitmap croped;
				int finalHeight = BitmapHelper.convertDPtoPX(174, mBaseActivity);

				// detect faces
				float top = photo.getHeight();
				FaceDetector detector = new FaceDetector(photo.getWidth(), photo.getHeight(), 5);
				Face[] faces = new Face[6];
				int facesCount = detector.findFaces(photo, faces);
				for (int i = 0; i < facesCount; i++) {
					if (faces[i] == null)
						continue;

					PointF point = new PointF();
					faces[i].getMidPoint(point);
					float y = point.y - faces[i].eyesDistance();
					if (y < top)
						top = y;
				}

				if (facesCount > 0) {
					if (top + finalHeight > photo.getHeight())
						top = photo.getHeight() - finalHeight;
					croped = Bitmap.createBitmap(photo, 0, (int) top, photo.getWidth(), finalHeight);
				} else {
					int half = photo.getHeight() / 2 - finalHeight / 2;
					if (photo.getHeight() <= finalHeight)
						half = 0;
					croped = Bitmap.createBitmap(photo, 0, half, photo.getWidth(), finalHeight);
				}
				return croped;
			} catch (Exception e) {
				return photo;
			}
		}
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if(values[0] == -1) { 
			mBaseActivity.setActionBarButtonInvisible(this);
			mBaseActivity.setActionBarInfoText(mBaseActivity.getString(R.string.connection_unavailable), this);
			return;
		}
		mBaseActivity.setActionBarInfoText(mBaseActivity.getString(R.string.navbar_caching)+" "+values[0]+"/"+ mArtistsList.size(), this);
		super.onProgressUpdate(values);
	}
	
	@Override
	protected void onPostExecute(Void result) {
		if(running || mBaseActivity == null) return;
		mBaseActivity.hideActionBarInfo(this);

	}
	
	public static class CommunicationException extends Exception {

		private static final long serialVersionUID = 1L;

	}
	
}
