package com.soundcloud.android.task;

import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.EventsWrapper;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

public class LoadDetailsTask extends LoadTask {
	
	 
		private final static String TAG = "LoadDetailsTask";
		JSONObject returnObject;
		@Override
		protected Boolean doInBackground(HttpUriRequest... params) {
			try{
				InputStream is = mActivityReference.get().getSoundCloudApplication().executeRequest(params[0]);
				ObjectMapper mapper = new ObjectMapper();
				
				if (isCancelled()) {
					return false;
				}
				
				Parcelable parcelable = null;
				switch (loadModel){
					case track:
						parcelable = mapper.readValue(is, Track.class);
						break;
					case user:
						parcelable = mapper.readValue(is, User.class);
						break;
					}
				
					if (mActivityReference.get() != null) mActivityReference.get().resolveParcelable(parcelable);
					publishProgress(parcelable);
				
					return true;
			} catch (Exception e) {
				//if (mActivityReference.get() != null) mActivityReference.get().setException(e);
			}
			return false;
		}
		
		@Override
		protected void onProgressUpdate(Parcelable... updates) {
			if (mActivityReference.get() != null)
				(mActivityReference.get()).mapDetails(updates[0]);
			
			mapDetails(updates[0]);
		}
		
		protected void mapDetails(Parcelable update){
			
		}

	
	
}