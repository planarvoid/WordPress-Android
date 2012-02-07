package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

public abstract class LoadTask<Model extends Parcelable> extends AsyncTask<Request, Void, Model> {
    private AndroidCloudAPI mApi;
    private Class<? extends Model> mModel;

    public LoadTask(AndroidCloudAPI api, Class<? extends Model> model) {
        mApi = api;
        mModel = model;
    }

    protected WeakReference<ScActivity> mActivityReference;

    public void setActivity(ScActivity activity) {
        if (activity != null) {
            mActivityReference = new WeakReference<ScActivity>(activity);
        }
    }

    @Override
    protected void onPostExecute(Model result) {
        final ScActivity activity = mActivityReference == null ? null : mActivityReference.get();
        if (activity != null) {
            activity.setProgressBarIndeterminateVisibility(false);
        }
    }

    @Override
    protected Model doInBackground(Request... request) {
        if (request == null || request.length == 0) throw new IllegalArgumentException("need path to executeAppendTask");

        try {
            HttpResponse resp = mApi.get(request[0]);
            if (isCancelled()) return null;

            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Model model = mApi.getMapper().readValue(resp.getEntity().getContent(), mModel);
                if (mApi instanceof Context && model != null){
                    updateLocally(((Context) mApi).getContentResolver(), model);
                }
                return model;
            } else {
                Log.w(TAG, "unexpected response " + resp.getStatusLine());
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return null;
        }
    }

    abstract protected void updateLocally(ContentResolver resolver, Model model);
}
