package com.soundcloud.android;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.soundcloud.android.CloudUtils.Model;
import com.soundcloud.android.objects.Event;

public class EventsAdapterWrapper extends LazyEndlessAdapter {
	private static String TAG = "AdpEvents";
	
	protected String mNextEventsParams;

	public EventsAdapterWrapper(LazyActivity activity, LazyBaseAdapter wrapped,
			String url, Model loadModel, String collectionKey) {
		super(activity, wrapped, url, loadModel, collectionKey);
	}
	
	@Override
	public void clear(){
		mNextEventsParams = "";
		super.clear();
	}
	
	@Override
	public String saveExtraData()
	{
		return mNextEventsParams;
	}
	
	@Override
	public void restoreExtraData(String restore)
	{
		mNextEventsParams = restore;
	}
	
	
	@Override
	protected String getUrl(){
		Log.i("DEBUG","Getting url " + mNextEventsParams);
		if (CloudUtils.stringNullEmptyCheck(mNextEventsParams))
			return CloudCommunicator.PATH_MY_ACTIVITIES;
		else
			return CloudCommunicator.PATH_MY_ACTIVITIES + mNextEventsParams;
	}
	
	@Override
	public void onDataNode(JSONObject data) {
		Log.i(TAG,"ON DATA NODE " + data.toString());
		
		try {
			mNextEventsParams = data.getString(Event.key_next_href);
			mNextEventsParams = mNextEventsParams.substring(mNextEventsParams.indexOf("?"));
		} catch (JSONException e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.i(TAG,"Error getting next url " + e.toString());
		}
	}

}
