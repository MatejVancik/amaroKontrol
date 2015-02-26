package com.mv2studio.amarok.kontrol.photosbrowser;

import android.content.Context;

import com.mv2studio.amarok.kontrol.helpers.CommonHelper;
import com.mv2studio.amarok.kontrol.tasks.GetCollectionPhotosTask.CommunicationException;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class LastFmParser {

	private String artist;
	private String apiKey = "2b4245fbf37ec558fcd0c688725f2134";
	private Context context;
	
	public LastFmParser(String artist, Context context) {
		this.context = context;
        this.artist = artist;
	}
	
	public String getLink() throws CommunicationException {
		String json = getJSON();

		if(json.isEmpty()) return null;
		
		try {
			JSONObject mainObj = new JSONObject(json);
			JSONArray pictsArray = mainObj.getJSONObject("artist").getJSONArray("image");
			for(int i = 0; i < pictsArray.length(); i++) {
				if(pictsArray.getJSONObject(i).getString("size").equals("mega")){
					return pictsArray.getJSONObject(i).getString("#text");
				}
			}
		} catch (JSONException e) {
		}
		
		return "";
		
	}

	private String getJSON() throws CommunicationException {
		if(!CommonHelper.isOnline(context)) throw new CommunicationException();
		
        String cleanArtist = "";
		try {
			cleanArtist = URLEncoder.encode(artist, "UTF-8");
		} catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
		}

		String url = "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist="+cleanArtist+"&api_key="+apiKey+"&format=json&autocorrect=1";

        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            return "";
        }
    }
	
}
