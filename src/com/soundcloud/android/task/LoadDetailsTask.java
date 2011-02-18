
package com.soundcloud.android.task;

import android.util.Log;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;

import android.os.Parcelable;

import java.io.InputStream;

public class LoadDetailsTask extends LoadTask {

    private final static String TAG = "LoadDetailsTask";

    @Override
    protected Boolean doInBackground(HttpUriRequest... params) {
        try {
            InputStream is = mActivityReference.get().getSoundCloudApplication().executeRequest(
                    params[0]);

            if (isCancelled()) {
                return false;
            }

            ObjectMapper mapper = mActivityReference.get().getSoundCloudApplication().getMapper();
            
            Parcelable parcelable = null;
            switch (loadModel) {
                case track:
                    parcelable = mapper.readValue(is, Track.class);
                    break;
                case user:
                    parcelable = mapper.readValue(is, User.class);
                    break;
            }

            if (mActivityReference.get() != null && parcelable != null) {
                CloudUtils.resolveParcelable(mActivityReference.get(), parcelable);
            }

            publishProgress(parcelable);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Parcelable... updates) {
        if (mActivityReference.get() instanceof Dashboard) {
          ((Dashboard)  (mActivityReference.get())).mapDetails(updates[0]);
        }

        mapDetails(updates[0]);
    }

    protected void mapDetails(Parcelable update) {
    }

}
