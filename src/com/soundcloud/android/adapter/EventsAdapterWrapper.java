package com.soundcloud.android.adapter;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.CloudUtils.Model;
import com.soundcloud.android.activity.LazyActivity;
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
		if (CloudUtils.stringNullEmptyCheck(mNextEventsParams))
			return super.getUrl();
		else
			return super.getUrl() + mNextEventsParams;
	}
	
	@Override
	public void onDataNode(JSONObject data) {
		try {
			mNextEventsParams = data.getString(Event.key_next_href);
			mNextEventsParams = mNextEventsParams.substring(mNextEventsParams.indexOf("?"));
		} catch (JSONException e) {
			//e.printStackTrace();
			//Log.i(TAG,"Error getting next url " + e.toString());
		}
	}

}
