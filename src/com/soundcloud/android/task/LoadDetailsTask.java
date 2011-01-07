package com.soundcloud.android.task;

import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

public class LoadDetailsTask extends LoadTask {
	
	 
		private final static String TAG = "LoadDetailsTask";
		JSONObject returnObject;
		@Override
		protected Boolean doInBackground(String... params) {
			if (mUrl == null || mUrl == "")
				return false;
			
			Log.i(TAG,mUrl);
			
		
			try {
		
				//String jsonRaw = activity.mCloudComm.getContent(mUrl);
				InputStream is = activity.getSoundCloudApplication().getContent(mUrl);
				String jsonRaw = CloudUtils.formatContent(is);
				Log.i(TAG,"On Details:" + jsonRaw);
				if (CloudUtils.getErrorFromJSONResponse(jsonRaw) != ""){
					if (activity != null) activity.setError(CloudUtils.getErrorFromJSONResponse(jsonRaw));
					return false;
				}
				try {
					returnObject = new JSONObject(jsonRaw);			
			
					if (isCancelled()) {
						return false;
					}
					
					switch (loadModel){
						case track:
							Track trk = new Track(returnObject);
							activity.resolveParcelable(trk);
							publishProgress(trk);	
							break;
						case user:
							User usr = new User(returnObject);
							activity.resolveParcelable(usr);
							publishProgress(usr);	
							break;
						case comment:
							Comment cmt = new Comment(returnObject);
							activity.resolveParcelable(cmt);
							publishProgress(cmt);	
							break;
						case event:
							Event evt = new Event(returnObject);
							activity.resolveParcelable(evt);
							publishProgress(evt);	
							break;
					}
				
				
					
				} catch (JSONException e) {
					Log.i(getClass().getName(),e.toString());
				}
				
				return true;
			} catch (Exception e) {
				if (activity != null) activity.setException(e);
			}
			return false;
		}
		
		@Override
		protected void onProgressUpdate(Parcelable... updates) {
			if (activity != null)
				(activity).mapDetails(updates[0]);
			
			mapDetails(updates[0]);
		}
		
		protected void mapDetails(Parcelable update){
			
		}
	
	
	
	
	
}