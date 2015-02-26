package com.mv2studio.amarok.kontrol.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.mv2studio.amarok.kontrol.R;

import java.lang.ref.WeakReference;


public class BitmapConcurrencyHandler {

	protected LruCache<String, Bitmap> cache;
	Context context;
	private static Bitmap emptyCover;
	Animation anim_in;
	
	public BitmapConcurrencyHandler(Context context) {
		this.context = context;
		emptyCover = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_collection_artist1);
		
		
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 16 ;
		cache = new LruCache<String, Bitmap>(cacheSize){
			@SuppressLint("NewApi")
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 12)
		            return bitmap.getByteCount() / 1024;
		        else
		            return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
			}
		};
	}
	
	
	
	private void addBitmapToMemoryCache(String key, Bitmap bitmap) {		
	    if (getBitmapFromMemCache(key) == null) {
	    	// create scaled version of bitmap
	        cache.put(key, bitmap);
	    }
	}

	private Bitmap getBitmapFromMemCache(String key) {
		Bitmap ret;
		try {
			ret = cache.get(key);
		} catch (NullPointerException ex) {
			ret = null;
		}
	    return ret; 
	}
	
	
	public void loadBitmap(String key, ImageView imageView) {
		Bitmap cached = getBitmapFromMemCache(key);
		if(cached != null) {
			imageView.setImageBitmap(cached);
		} else if (cancelPotentialWork(key, imageView)) {
	        final BitmapWorkerTask task = new BitmapWorkerTask(imageView, key);
	        final AsyncDrawable asyncDrawable = new AsyncDrawable(task);
	        imageView.setImageDrawable(asyncDrawable);
	        task.execute();
	    }
	}
	
	
	public static boolean cancelPotentialWork(String key, ImageView imageView) {
	    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

	    if (bitmapWorkerTask != null) {
	    	String bitmapUrl = null;
        	bitmapUrl = bitmapWorkerTask.key;
        	
	        if ((bitmapUrl == null) || (!bitmapUrl.equals(key))) {
	        	bitmapWorkerTask.cancel(true);
	        } else {
	            // The same URL is already being downloaded.
	            return false;
	        }
	    }
	    // No task associated with the ImageView, or an existing task was cancelled
	    return true;
	}
	
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		@SuppressWarnings("deprecation")
		public AsyncDrawable(BitmapWorkerTask bitmapWorkerTask) {
			super(emptyCover);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}
	

	
	
	
	
	
	
	
	
	
	
	
	private class BitmapWorkerTask extends AsyncTask<Long, Void, Bitmap> {

		public String key;
		private final WeakReference<ImageView> imageViewReference;
		Animation anim_in = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);

		public BitmapWorkerTask(ImageView imageView, String key) {
			// Use a WeakReference to ensure the ImageView can be garbage
			// collected
			imageViewReference = new WeakReference<ImageView>(imageView);
			this.key = key;
		}

		// Decode image in background.
		@Override
		protected Bitmap doInBackground(Long... params) {
			Bitmap loaded = null;
			loaded = MediaHelper.readFromCache(key, context);
			if(loaded == null) return null;
			loaded = BitmapHelper.scaleToFill(loaded, context.getResources().getInteger(R.integer.artist_mini_photo_dimen), context.getResources().getInteger(R.integer.artist_mini_photo_dimen));
			
			if (loaded != null) addBitmapToMemoryCache(key, loaded);
			
			return loaded;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}
			if (imageViewReference != null && bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

				if (this == bitmapWorkerTask && imageView != null) {
					imageView.startAnimation(anim_in);
					imageView.setImageBitmap(bitmap);
				}
			}
		}
	}
	
}
