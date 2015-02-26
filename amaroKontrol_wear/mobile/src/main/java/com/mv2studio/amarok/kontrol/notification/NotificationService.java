//package com.mv2studio.amarok.kontrol.notification;
//
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.wifi.WifiManager;
//import android.net.wifi.WifiManager.WifiLock;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.PowerManager;
//import android.preference.PreferenceManager;
//import android.support.v4.content.LocalBroadcastManager;
//import android.util.Log;
//
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.wearable.Asset;
//import com.google.android.gms.wearable.DataApi;
//import com.google.android.gms.wearable.Node;
//import com.google.android.gms.wearable.PutDataMapRequest;
//import com.google.android.gms.wearable.PutDataRequest;
//import com.google.android.gms.wearable.Wearable;
//import com.mv2studio.amarok.kontrol.Prefs;
//import com.mv2studio.amarok.kontrol.R;
//import com.mv2studio.amarok.kontrol.communication.amarok.tasks.AmarokCommandTask;
//import com.mv2studio.amarok.kontrol.helpers.CommonHelper;
//import com.mv2studio.amarok.kontrol.helpers.MediaHelper;
//import com.mv2studio.amarok.kontrol.shared.model.Song;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.ByteArrayOutputStream;
//import java.util.concurrent.Executor;
//
//public class NotificationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
//
//	public AmarokNotification notify;
//
//	Handler handler;
//	Runnable updateTask;
//	Executor exe = new ThreadPerTaskExecutor();
//	private int ignoreCount = 0, IGNORE_TOTAL = 2;
//
//	public static Song currentSong = new Song("","","");
//
//	public static final String INTENT_BASE_NAME = "com.mv2studio.amarok.kontrol";
//
//
//	private static String songInfo = "/getCurrentSongJson/";
//	private static String lyricsKey = "/getLyrics/";
//	private static String cover = "/getCurrentCover/" + Prefs.getScreenWidth();
//	private WifiLock lock;
//
//	public static final String
//                               I_TRACK = "TRACK",
//							   I_ARTIST = "ARTIST",
//							   I_ALBUM = "ALBUM",
//							   I_LENGTH = "LENGTH",
//							   I_POSITION = "POSITION",
//							   I_PLAYING_STATE = "PLAYING_STATE",
//							   I_LYRICS = "LYRICS",
//							   I_UPDATE_PLAYLIST = "UPDATE_PLAYLIST",
//							   I_COVER = "COVER",
//							   I_BLURED_BCG = "BLURED",
//							   STOP_FOREGROUND = "STOP_FOREGROUND",
//							   START_FOREGROUND = "START_FOREGROUND",
//							   OLD_SCRIPT = "OLD_SCRIPT",
//							   I_NEXT = "next",
//							   I_PREV = "prev",
//							   I_EXIT = "exit",
//							   I_PLAY = "play",
//							   I_FULL_UPDATE = "FULL_UPDATE";
//
//	public static final String MAIN_ACTIVITY = "";
//
//
//    LocalBroadcastManager broadcaster;
//    NotificationClick reciever;
//
//    GoogleApiClient googleApiClient;
//
//
//	@Override
//	public IBinder onBind(Intent intent) {
//		return null;
//	}
//
//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		return START_NOT_STICKY;
//	}
//
//
//	@Override
//	public void onCreate() {
//		super.onCreate();
//		currentSong = new Song("","","");
//		Prefs.setNotificationPrefs(PreferenceManager.getDefaultSharedPreferences(this));
//
//		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//		lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Lock");
//		lock.acquire();
//
//		handler = new Handler();
//		updateTask = new Runnable() {
//
//			@Override
//			public void run() {
//				PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//
//			    if (!pm.isScreenOn()) {
//			    	handler.postDelayed(updateTask, notify.isForeground() ? Prefs.notifyUpdateInterval*1000 : Prefs.updateInterval*1000);
//			        return;
//			    }
//
//				// set Prefs
//			    Prefs.setNotificationPrefs(PreferenceManager.getDefaultSharedPreferences(NotificationService.this));
//				Prefs.setScreenWidth(NotificationService.this);
//
//				// check the global background data setting
//				if(!CommonHelper.isWifiConnected(NotificationService.this) && !Prefs.use3g) {
//					// disable service if lost connection in foreground
//					if(notify.isForeground()) {
//						stopSelf();
//						return;
//					}
//
//					notify.stopForeground();
//					// send info
//					exe.execute(connectionUnavailableTask);
//				} else {
//					exe.execute(mainTask);
//				}
//				handler.postDelayed(updateTask, notify.isForeground() ? Prefs.notifyUpdateInterval*1000 : Prefs.updateInterval*1000);
//			}
//		};
//		broadcaster = LocalBroadcastManager.getInstance(this);
//
//		notify = new AmarokNotification(this);
//
//		IntentFilter intentFilter = new IntentFilter();
//		intentFilter.addAction(STOP_FOREGROUND);
//		intentFilter.addAction(START_FOREGROUND);
//		intentFilter.addAction(I_FULL_UPDATE);
//		intentFilter.addAction(I_PLAY);
//		intentFilter.addAction(I_PREV);
//		intentFilter.addAction(I_NEXT);
//		intentFilter.addAction(I_EXIT);
//		reciever = new NotificationClick();
//		registerReceiver(reciever, intentFilter);
//		updateTask.run();
//
//        googleApiClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .addApi(Wearable.API)
//                .build();
//
//        googleApiClient.connect();
//	}
//
//	public void onDestroy() {
//		super.onDestroy();
//		notify.stopForeground();
//		handler.removeCallbacks(updateTask);
//		lock.release();
//		unregisterReceiver(reciever);
//	}
//
//	Runnable connectionUnavailableTask = new Runnable() {
//
//		@Override
//		public void run() {
//			Intent intent = new Intent(MAIN_ACTIVITY);
//
//			String album = getString(R.string.enable_3g),
//				   title = getString(R.string.please_turn_on_wifi);
//
//			Song newSong = new Song(title, "", album);
//			if (!currentSong.equals(newSong)) {
//				currentSong = newSong;
//
//				Bitmap coverArt = BitmapFactory.decodeResource(NotificationService.this.getResources(), R.drawable.album),
//					   blured = BitmapFactory.decodeResource(NotificationService.this.getResources(), R.drawable.albumblured);
//
//				intent.putExtra(I_BLURED_BCG, blured);
//				intent.putExtra(I_COVER, coverArt);
//			}
//
//			intent.putExtra(I_TRACK, title);
//			intent.putExtra(I_ALBUM, album);
//
//			broadcaster.sendBroadcast(intent);
//		}
//	};
//
//	Runnable mainTask = new Runnable() {
//		Intent intent = new Intent(MAIN_ACTIVITY);
//		@Override
//		public void run() {
//				final String address = Prefs.getIp();
//
//				// Download data
//				String json = CommonHelper.getStringFromHttp(address + songInfo);
//
//				if (json.equals("NACK")) {
//					intent.putExtra(OLD_SCRIPT, true);
//				}
//
//				String title = null, artist = null, album = null;
//				int id = 0, length = 0, position = 0, playingState = 3;
//				try {
//					JSONObject main = new JSONObject(json);
//
//					// get track details
//					JSONObject track = main.getJSONObject("currentTrack");
//					title = track.getString("title");
//					artist = track.getString("artist");
//					album = track.getString("album");
//
//					id = track.getInt("id");
//					length = track.getInt("length");
//					position = track.getInt("position");
//					playingState = track.getInt("status");
//
//				} catch (JSONException e) { // AMAROK OFF / SOME ERROR
//					playingState = 3;
//				}
//
//				boolean playingStateChanged = playingState != PlayingState.state.ordinal();
//
//			// set current state
//			switch(playingState) {
//				case 0: PlayingState.state = PlayingState.PLAYING; ignoreCount = 0; break;
//				case 1: PlayingState.state = PlayingState.PAUSE; ignoreCount = 0; break;
//				case 2: PlayingState.state = PlayingState.NOTPLAYING;
//						// DEAL WITH SCRIPT BUG (IGNORE NOTPLAYING)
//						if(ignoreCount++ < IGNORE_TOTAL) return;
//
//						title = getString(R.string.not_playing);
//						artist = getString(R.string.press_play);
//						album = getString(R.string.click_playlist_item);
//						break;
//				default: PlayingState.state = PlayingState.DOWN;
//						title = getString(R.string.not_connected);
//						artist = getString(R.string.amarok_not_on);
//						album = getString(R.string.amarok_bad_ip);
//						ignoreCount = 0;
//						break;
//			}
//
//			//state changed
//			if(playingStateChanged) {
//				notify.setPlayPauseButton();
//			}
//
//			Bitmap coverArt = null;
//			Bitmap bluredCover = null;
//			Song newSong = new Song(id, title, artist, album);
//
//			if (currentSong.equals(newSong)){
//				// do not update anything
////				return null;
//
//			} else {
//
//				// set current song / download bitmap if new song
//				currentSong = newSong;
//				if(PlayingState.state != PlayingState.DOWN) {
//					coverArt = MediaHelper.downloadBitmap(address + cover, artist + album, (Context) NotificationService.this, true);
//				}
//
//				bluredCover = MediaHelper.getBluredBitmap(artist + album, coverArt, (Context) NotificationService.this);
//				if (coverArt == null) {
//					coverArt = BitmapFactory.decodeResource(NotificationService.this.getResources(), R.drawable.album);
//				}
//
//                currentSong.setCover(coverArt);
//                currentSong.setBluredCover(bluredCover);
//
//				intent.putExtra(I_COVER, coverArt);
//
//				// if lyrics
//				if(getResources().getBoolean(R.bool.IsTablet)) {
//					String lyrics = CommonHelper.getStringFromHttp(address + lyricsKey + currentSong.getId());
//					currentSong.setLyrics(lyrics);
//					intent.putExtra(I_LYRICS, lyrics);
//				}
//
//                new DataTask().execute();
//
//				// notify activity to update playlist
//				Intent i = new Intent(MAIN_ACTIVITY);
//				i.putExtra(I_UPDATE_PLAYLIST, true);
//				i.putExtra(I_BLURED_BCG, bluredCover);
//				broadcaster.sendBroadcast(i);
//				notify.updateNotification();
//			}
//
//			// send song detail broadcast
//			intent.putExtra(I_ARTIST, artist);
//			intent.putExtra(I_TRACK, title);
//			intent.putExtra(I_ALBUM, album);
//			intent.putExtra(I_LENGTH, length);
//			intent.putExtra(I_POSITION, position);
//			intent.putExtra(I_PLAYING_STATE, playingState);
//
//			broadcaster.sendBroadcast(intent);
//		}
//	};
//
//    @Override
//    public void onConnected(Bundle bundle) {
//        new DataTask().execute();
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//
//    }
//
//    @Override
//    public void onConnectionFailed(ConnectionResult connectionResult) {
//
//    }
//
//
//
//
//
//
//
//    private static final String NEW_SONG = "/amarokontrol/new_song";
//    private static final String SONG_CONTENT = "content";
//    private static final String SONG_COVER = "/cover";
//    private static final String SONG_COVER_MAIN = "cover_art";
//    private static final String SONG_COVER_BLURED = "cover_blured";
//
//    class DataTask extends AsyncTask<Node, Void, Void> {
//
//        @Override
//        protected Void doInBackground(Node... nodes) {
//
//            PutDataMapRequest dataMap = PutDataMapRequest.create(NEW_SONG);
//            dataMap.getDataMap().putStringArray(SONG_CONTENT,
//                    new String[]{currentSong.getArtist(), currentSong.getTitle(), currentSong.getAlbum()});
//
//            Log.e("", "Sending cover " + currentSong.getCover() + "  blu " + currentSong.getBlured());
//
//            if(currentSong.getCover() != null)
//                dataMap.getDataMap().putAsset(SONG_COVER_MAIN, createAssetFromBitmap(currentSong.getCover()));
//
//            if(currentSong.getBlured() != null)
//                dataMap.getDataMap().putAsset(SONG_COVER_BLURED, createAssetFromBitmap(currentSong.getBlured()));
//
//            PutDataRequest request = dataMap.asPutDataRequest();
//
//            DataApi.DataItemResult dataItemResult = Wearable.DataApi
//                    .putDataItem(googleApiClient, request).await();
//
//
//            Log.d("[DEBUG] SendDataCoolTask - doInBackground", "/myapp/myeventstatus "+getStatus());
//            return null;
//        }
//    }
//
//    private static Asset createAssetFromBitmap(Bitmap bitmap) {
//        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
//        return Asset.createFromBytes(byteStream.toByteArray());
//    }
//
//	public class NotificationClick extends BroadcastReceiver {
//
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			if(intent.getAction().equals("next")) {
//				new AmarokCommandTask().execute(AmarokCommandTask.next);
//			} else if (intent.getAction().equals("prev")) {
//				new AmarokCommandTask().execute(AmarokCommandTask.prev);
//			} else if(intent.getAction().equals("play")) {
//				new AmarokCommandTask().execute(AmarokCommandTask.playPause);
//			} else if(intent.getAction().equals("exit")) {
//				stopSelf();
//			} else if(intent.getAction().equals(START_FOREGROUND)) {
//				notify.startForeground();
//			} else if(intent.getAction().equals(STOP_FOREGROUND)) {
//				notify.stopForeground();
//			} else if(intent.getAction().equals(I_FULL_UPDATE)) {
//				currentSong = new Song("", "", "");
//				exe.execute(mainTask);
//			}
//		}
//	}
//
//	class ThreadPerTaskExecutor implements Executor {
//		public void execute(Runnable r) {
//			new Thread(r).start();
//
//		}
//	}
//
//}
