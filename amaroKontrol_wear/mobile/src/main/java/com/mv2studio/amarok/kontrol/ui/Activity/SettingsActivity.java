package com.mv2studio.amarok.kontrol.ui.Activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.mv2studio.amarok.kontrol.App;
import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.helpers.FileHelper;

import java.io.File;

public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager()
			.beginTransaction()
			.replace(android.R.id.content, new SettingsFragment())
			.commit();
	}
	
	@Override
	protected void onResume() {
		App.activityResumed();
		super.onResume(); 
	}
	
	@Override
	protected void onPause() {
		App.activityPaused();
		super.onPause();
	}
		
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	

	
	
	public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{
		
		SharedPreferences prefs;
		
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.preferences);
	        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			getPreferenceScreen().findPreference(Prefs.notifyUpdateIntervalKey).setEnabled(Prefs.showNotify);
			getPreferenceScreen().findPreference(Prefs.notifyShowPhotoKey).setEnabled(Prefs.showNotify);
			getPreferenceScreen().findPreference(Prefs.volumeStepKey).setEnabled(Prefs.useVolButtons);
	    }

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
				Preference preference) {
			if(preference.getKey().equals(Prefs.clearCacheKey) && FileHelper.isExternalStorageWritable()) {
				new AsyncTask<Void, Void, Boolean>(){
					@Override
					protected void onPreExecute() {Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.pref_clean_cache_running), Toast.LENGTH_SHORT).show();};
					@Override
					protected Boolean doInBackground(Void... params) {					
						Boolean ret = true;
						File[] files = getActivity().getExternalCacheDir().listFiles();
						for(File file: files) {
							ret &= file.delete();
						}
						return ret;
					}
					@Override
					protected void onPostExecute(Boolean result) {
						// TODO: NULL POINTER pri odchode z aktivity
						Toast.makeText(getActivity(), getActivity().getResources().getString(result ? R.string.pref_clean_cache_OK : R.string.pref_clean_cache_error), Toast.LENGTH_LONG).show();
					}
				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
				
			}
			return super.onPreferenceTreeClick(preferenceScreen, preference);
		}
		
		@Override
		public void onDetach() {
			Prefs.setAll(prefs);
			super.onDetach();
		}


		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if(key.equals(Prefs.ipPortKey)) {
				Prefs.setIp(prefs); 
			} else if(key.equals(Prefs.useVolButtonsKey)) {
				Prefs.useVolButtons = prefs.getBoolean(key, true);
				getPreferenceScreen().findPreference(Prefs.volumeStepKey).setEnabled(Prefs.useVolButtons);
			} else if(key.equals(Prefs.showNotifyKey)) {
				Prefs.showNotify = prefs.getBoolean(key, true);
				getPreferenceScreen().findPreference(Prefs.notifyUpdateIntervalKey).setEnabled(Prefs.showNotify);
				getPreferenceScreen().findPreference(Prefs.notifyShowPhotoKey).setEnabled(Prefs.showNotify);
			} else if(key.equals(Prefs.updateIntervalKey)) {
				Prefs.updateInterval = Integer.parseInt(prefs.getString(key, "3"));
			} else if(key.equals(Prefs.notifyUpdateIntervalKey)) {
				Prefs.notifyUpdateInterval = Integer.parseInt(prefs.getString(key, "5"));
			} else if(key.equals(Prefs.volumeStepKey)) {
				Prefs.volumeStep = Integer.parseInt(prefs.getString(key, "5"));
			} else if(key.equals(Prefs.use3gKey)) {
				Prefs.use3g = prefs.getBoolean(key, false);
			} else if(key.equals(Prefs.blurKey)) {
				Prefs.blurIntensity = Integer.parseInt(prefs.getString(key, "20"));
				// clear blured
				if(FileHelper.isExternalStorageWritable()) {
					new AsyncTask<Void, Void, Void>(){
						@Override
						protected Void doInBackground(Void... params) {
							File[] files = getActivity().getExternalCacheDir().listFiles();
							for(File file: files) {
								if(file.getName().endsWith("blured.jpg")) file.delete();
							}
							return null;
						}
					}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void)null);
				}
			}
		}
		
		@Override
		public void onResume() {
		    super.onResume();
		    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onPause() {
		    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		    super.onPause();
		}
	}
	
	
	
	
	
}
