package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.DBAdapter;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.BaseObj.WriteState;

public class CommitTracksTask extends AsyncTask<Track, Parcelable, Boolean> {
	
	private SoundCloudApplication scApp;
	private long currentUserId;
	protected DBAdapter db;
	protected Track[] tracks;
	
	public CommitTracksTask(SoundCloudApplication scApp, Long userId){
		this.scApp = scApp;
		
		if (userId != null)
			this.currentUserId = userId;
	}
	
	@Override
	protected void onPreExecute() {
		Log.i(getClass().getName(),"Starting playlist commit");
		db = new DBAdapter(scApp);
		db.open();
	}

	@Override
	protected void onProgressUpdate(Parcelable... updates) {
	}

	
	
	@Override
	protected Boolean doInBackground(Track... params) {

		for (int i = 0; i < params.length; i++){
			CloudUtils.resolveTrack(scApp, params[i], WriteState.all, currentUserId);
		}
		
		afterCommitInBg();
		
		return true;
		
	}
	
	protected void afterCommitInBg(){
		
	}

	@Override
	protected void onPostExecute(Boolean result) {
		db.close();
		Log.i(getClass().getName(),"Done playlist commit");
	}
	
}