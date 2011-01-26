package com.soundcloud.android.task;

import java.lang.ref.WeakReference;

import org.apache.http.client.methods.HttpUriRequest;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.activity.LazyActivity;

public abstract class LoadTask extends AsyncTask<HttpUriRequest, Parcelable, Boolean> {
	private final static String TAG = "LoadTask";
	
	protected WeakReference<LazyActivity> mActivityReference;
	protected WeakReference<Context> mAppContextReference;
	public CloudUtils.Model loadModel;
	
	protected boolean mCancelled;


	@Override
	protected void onPreExecute() {
		mCancelled = false;
		
		
		System.gc();
	}
	
	public void setActivity(LazyActivity activity){
		
		mActivityReference = new WeakReference<LazyActivity>(activity);

		if (activity != null){
			mAppContextReference = new WeakReference<Context>(activity.getApplicationContext());
			activity.setException(null);
		}
		
	}
	
	@Override
	protected void onProgressUpdate(Parcelable... updates) {}

	@Override
	protected void onPostExecute(Boolean result) {
		
		//activity.handleException();
		//activity.handleError());
		if (mActivityReference.get() != null) mActivityReference.get().setProgressBarIndeterminateVisibility(false);
		
		System.gc();
	}

}