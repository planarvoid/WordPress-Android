package com.soundcloud.android.task;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Comment;

public class LoadCommentsTask extends AsyncTask<String, Parcelable, Boolean> {
	
	public int track_id;
	public SoundCloudApplication soundcloudApplication;
	
	protected boolean mCancelled;
	protected boolean mFinished;
	
	private ArrayList<Comment> commentsList;
	private Comment[] comments;
	
	private HashMap<String, String> threadData = null;
	private ArrayList<HashMap<String, String>> commentData = null;
	private String lastUserPermalink = "";
	
	private int commentsPerPage = 50;
	private int commentsPageIndex = 0;
	
	
	
	@Override
	protected void onPreExecute() {
		commentsList = new ArrayList<Comment>();
		mCancelled = false;
		mFinished = false;
		
		System.gc();
	}

	@Override
	protected void onProgressUpdate(Parcelable... updates) {
		commentsList.add((Comment) updates[0]);
	}

	
	
	@SuppressWarnings("unchecked")
	@Override
	protected Boolean doInBackground(String... params) {
		
		String mBaseUrl = CloudUtils.buildRequestPath(SoundCloudApplication.PATH_TRACK_COMMENTS.replace("{track_id}",Integer.toString(track_id)),null);
		
		Log.i("COMMENTS","Load comments task  " + mBaseUrl);
		
		while (!mFinished){
			Uri u = Uri.parse(mBaseUrl);
			Uri.Builder builder = u.buildUpon();
			builder.appendQueryParameter("offset", String.valueOf(commentsPerPage*(commentsPageIndex)));
			builder.appendQueryParameter("limit", String.valueOf(commentsPerPage));
			String mUrl = builder.build().toString();
			
			try {

				String jsonRaw = "";
				try {
					InputStream is = soundcloudApplication.getContent(mUrl);
					jsonRaw = CloudUtils.streamToString(is);
					//jsonRaw = mCloudComm.getContent(mUrl);
					if (CloudUtils.getErrorFromJSONResponse(jsonRaw) != ""){
						//TODO activity.setError(CloudCommunicator.getErrorFromJSONResponse(jsonRaw));
						return false;
					}	
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
				

				if (isCancelled())
					return false;
				
				
				JSONArray collection = new JSONArray(jsonRaw);				
				if (collection.length() == 0 || collection.length() != commentsPerPage){
					mFinished = true;
					
					if (collection.length() == 0) //done
						return true;
				}
				
				
				
				for (int i = 0; i < collection.length(); i++) {
					if (isCancelled())
						return false;
					
					
					
				/*	try {
						//publishProgress(new Comment(collection.getJSONObject(i)));
					} catch (JSONException e) {
						Log.i("COMMENTS",e.toString());
					}*/
				}
				
			} catch (JSONException e) {
				//TODO activity.setException(e);
				Log.i("COMMENTS",e.toString());
				return false;
			}
			
			commentsPageIndex++;
		}
		
		return true;
		
	}

	@Override
	protected void onPostExecute(Boolean result) {
		
		if (result){
			if (commentsList.size() > 0){
				int i = 0;
				comments = new Comment[commentsList.size()];
				for (Comment comment : commentsList){
					comments[i] = comment;
					i++;
				}
				setComments(comments,track_id);
			} else {
				setComments(new Comment[0],track_id);
			}
		} else {
			setComments(new Comment[0],track_id);
		}
		
		System.gc();
		
	}
	
	protected void setComments(Comment[] comments, int trackId){
		
	}
	
	
}