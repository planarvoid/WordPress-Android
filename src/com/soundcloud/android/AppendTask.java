package com.soundcloud.android;

import java.io.IOException;
import java.io.InputStream;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.R;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

/**
 * A background task that will be run when there is a need
 * to append more data. Mostly, this code delegates to the
 * subclass, to append the data in the background thread and
 * rebind the pending view once that is done.
 */
public class AppendTask extends AsyncTask<String, Parcelable, Boolean> {
	private static final String TAG = "AppendTask";

	private LazyEndlessAdapter mAdapter;
	private LazyActivity mActivity;
	
	private Boolean keepGoing = true;
	
	private Parcelable newItems[];
	
	public void setContext(LazyEndlessAdapter lazyEndlessAdapter,LazyActivity activity) {
		mAdapter = lazyEndlessAdapter;
		mActivity = activity;
	}
	
	@Override
	protected void onPreExecute() {
		if (mAdapter != null) mAdapter.onPreTaskExecute();
	}
	

	@Override
	protected void onPostExecute(Boolean keepGoing) {
		
		if (mAdapter == null || newItems == null)
			return;
		
		if (mAdapter != null) {
			if (newItems.length > 0){
				for (Parcelable newitem : newItems){
					mAdapter.getData().add(newitem);	
				}
			}
			
			mAdapter.onPostTaskExecute(keepGoing);
		}
		
	}

	@Override
	protected Boolean doInBackground(String... params) {

		
		String baseUrl = params[0];
		if (baseUrl == null || baseUrl == "")
			return false;
		
		Boolean keep_appending = true;
		JSONObject collectionHolder;
		JSONArray collection;
	
		try {
			
			Uri u = Uri.parse(baseUrl);
			Uri.Builder builder = u.buildUpon();
			builder.appendQueryParameter("rand", String.valueOf(((int) Math.random()*100000)));
			builder.appendQueryParameter("offset", String.valueOf(mActivity.getPageSize()*(mAdapter.getCurrentPage())));
			if (baseUrl.indexOf("limit") == -1) builder.appendQueryParameter("limit", String.valueOf(mActivity.getPageSize()));
			builder.appendQueryParameter("consumer_key", mActivity.getResources().getString(R.string.consumer_key));
			
			//String jsonRaw = activity.mCloudComm.getContent(mUrl);
			InputStream is = (InputStream) mActivity.getCloudComm().getContent(builder.build().toString());
			String jsonRaw = CloudCommunicator.formatContent(is);
			
			if (CloudCommunicator.getErrorFromJSONResponse(jsonRaw) != ""){
				if (mActivity != null) mActivity.setError(CloudCommunicator.getErrorFromJSONResponse(jsonRaw));
				return false;
			}
			
			Log.i("TASK","Collection key is " + mAdapter.getCollectionKey());

			if (mAdapter.getCollectionKey() != ""){
				collectionHolder = new JSONObject(jsonRaw);
				collection = collectionHolder.getJSONArray(mAdapter.getCollectionKey());
				mAdapter.onDataNode(collectionHolder);
			} else if (jsonRaw.startsWith("{")){
				collection = new JSONArray("["+jsonRaw+"]");
			} else {
				collection = new JSONArray(jsonRaw);	
			}
			
			if (collection.length() < mActivity.getPageSize())
				keep_appending = false;
			
			Log.i("TASK","Parsing collection about to " + collection.length());
			
			newItems = new Parcelable[collection.length()];
			
			for (int i = 0; i < collection.length(); i++) {
				
				
				try {
					switch (mAdapter.getLoadModel()){
						case track:
							Track trk = new Track(collection.getJSONObject(i));
							mActivity.resolveParcelable(trk);
							newItems[i] = trk;
							break;
						case user:
							User usr = new User(collection.getJSONObject(i));
							mActivity.resolveParcelable(usr);
							newItems[i] = usr;
							break;
						case comment:
							Comment cmt = new Comment(collection.getJSONObject(i));
							mActivity.resolveParcelable(cmt);
							newItems[i] = cmt;	
							break;
						case event:
							Event evt = new Event(collection.getJSONObject(i));
							mActivity.resolveParcelable(evt);
							newItems[i] = evt;
							break;
					}
				
				} catch (JSONException e) {
					Log.i(getClass().getName(),e.toString());
				}
			
			}
			
			mAdapter.incrementPage();
			
			return keep_appending;
			
		} catch (IOException e) {
			if (mActivity != null) mActivity.setException(e);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthMessageSignerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthExpectationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthCommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
		
	}
}