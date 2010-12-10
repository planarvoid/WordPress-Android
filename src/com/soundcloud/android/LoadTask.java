package com.soundcloud.android;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;

abstract class LoadTask extends AsyncTask<String, Parcelable, Boolean> {
	private final static String TAG = "LoadTask";
	
	protected LazyActivity activity;
	public String mUrl = null;
	
	public CloudUtils.Model loadModel;
	
	protected Context appContext;
	protected boolean mCancelled;


	@Override
	protected void onPreExecute() {
		mCancelled = false;
		
		
		System.gc();
	}
	
	public void setActivity(LazyActivity _activity){
		
		this.activity = _activity;

		if (activity != null){
			appContext = activity.getApplicationContext();
			activity.mException = null;
			activity.mError = "";
		}
		
	}
	
	@Override
	protected void onProgressUpdate(Parcelable... updates) {}

	@Override
	protected void onPostExecute(Boolean result) {
		
		activity.handleException();
		activity.setProgressBarIndeterminateVisibility(false);
		
		System.gc();
	}

	public Uri getLoadedURL() {
		if (mUrl != null) {
			return Uri.parse(mUrl);
		}
		return null;
	}
}