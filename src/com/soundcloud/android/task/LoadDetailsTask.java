
package com.soundcloud.android.task;

import com.soundcloud.android.CloudUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.map.ObjectMapper;

import android.os.Parcelable;
import android.util.Log;

import java.io.InputStream;

public class LoadDetailsTask extends LoadTask {

    private final static String TAG = "LoadDetailsTask";

    @Override
    protected Boolean doInBackground(HttpUriRequest... params) {
        try {
            InputStream is = mActivityReference.get().getSoundCloudApplication().execute(
                    params[0]).getEntity().getContent(); //XXX

            if (isCancelled()) {
                return false;
            }

            ObjectMapper mapper = mActivityReference.get().getSoundCloudApplication().getMapper();
            
            Parcelable parcelable = (Parcelable) mapper.readValue(is, loadModel);
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
        mapDetails(updates[0]);
    }

    protected void mapDetails(Parcelable update) {
    }

}
