package com.soundcloud.android.task;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

public abstract class LoadTask<Model extends Parcelable> extends AsyncTask<String, Parcelable, Model> {
    private final static String TAG = "LoadTask";
    private SoundCloudApplication mApi;
    private Class<?> mModel;

    public LoadTask(SoundCloudApplication api, Class<?> model) {
        mApi = api;
        mModel = model;
    }

    protected WeakReference<ScActivity> mActivityReference;

    public void setActivity(ScActivity activity) {
        if (activity != null) {
            mActivityReference = new WeakReference<ScActivity>(activity);
            activity.setException(null);
        }
    }

    @Override
    protected void onPostExecute(Model result) {
        final ScActivity activity = mActivityReference.get();
        if (activity != null) {
            activity.setProgressBarIndeterminateVisibility(false);
        }
    }

    @Override
    protected Model doInBackground(String... path) {
        try {
            HttpResponse resp = mApi.getContent(path[0]);
            if (isCancelled()) return null;

            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Model model = (Model) mApi.getMapper().readValue(resp.getEntity().getContent(), mModel);
                CloudUtils.resolveParcelable(mApi, model);
                publishProgress(model);
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
}
