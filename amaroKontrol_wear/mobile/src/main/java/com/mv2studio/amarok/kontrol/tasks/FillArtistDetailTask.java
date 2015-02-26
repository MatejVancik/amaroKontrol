package com.mv2studio.amarok.kontrol.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mv2studio.amarok.kontrol.Prefs;
import com.mv2studio.amarok.kontrol.R;
import com.mv2studio.amarok.kontrol.helpers.CommonHelper;
import com.mv2studio.amarok.kontrol.helpers.MediaHelper;
import com.mv2studio.amarok.kontrol.shared.model.Song;
import com.mv2studio.amarok.kontrol.ui.Activity.BaseActivity;
import com.mv2studio.amarok.kontrol.ui.Fragment.ArtistFragment.ArtistDetailAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class FillArtistDetailTask extends AsyncTask<Void, Void, ArrayList<Song>> {

	private String mCommand = "/getCollectionTracksByArtistIdJSON/";
    private Bitmap mBluredPhoto;
	private Context mContext;

    private String mArtistName;
    private Bitmap mArtistPhoto;

    private ListView mListView;
    private ImageView mHeaderPhoto;
    private TextView mHeaderText;

	public FillArtistDetailTask(ListView view, int artistId) {
		mListView = view;

		mCommand += artistId;
		mContext = view.getContext();

        ViewGroup header = (ViewGroup) mListView.getTag();
        mHeaderPhoto = (ImageView) header.findViewById(R.id.artist_detail_header_photo);
        mHeaderText = (TextView) header.findViewById(R.id.artist_detail_header_name);
    }

    @Override
	protected ArrayList<Song> doInBackground(Void... param) {
        // get data
		String songsJSON = CommonHelper.getStringFromHttp(Prefs.getIp() + mCommand);

        ArrayList<Song> songsList = new ArrayList<Song>();

		try { // parse JSON response
			JSONObject obj = new JSONObject(songsJSON);
			mArtistName = obj.getString("artist");
			JSONArray array = obj.getJSONArray("songs");

			for(int i = 0; i < array.length(); i++) {
				JSONObject songObj = array.getJSONObject(i);
				String title = songObj.getString("track"),
					   album = songObj.getString("albumName");
				int songID = songObj.getInt("trackID");
				songsList.add(new Song(songID, title, mArtistName, album));
			}

            mArtistPhoto = MediaHelper.readFromCache(mArtistName, mContext);
			if(mArtistPhoto != null) {
                mBluredPhoto = MediaHelper.getBluredBitmap(mArtistName, mArtistPhoto, mContext);
			}
		} catch (JSONException e) {
            e.printStackTrace();
		}

		return songsList;
	}
	
	@Override
	protected void onPostExecute(ArrayList<Song> result) {
		try {
            BaseActivity ba = (BaseActivity) mContext;
            ba.imageViewAnimatedChange(mBluredPhoto);
            mHeaderText.setText(mArtistName);

            if(mArtistPhoto != null) mHeaderPhoto.setImageBitmap(mArtistPhoto);
            else mHeaderPhoto.setImageResource(R.drawable.no_photo);

		} catch (NullPointerException ex) {
            ex.printStackTrace();}
	}
}
