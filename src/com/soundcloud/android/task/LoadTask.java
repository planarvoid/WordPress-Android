package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.utils.CloudUtils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

public abstract class LoadTask<Model extends Parcelable> extends AsyncTask<String, Parcelable, Model> {
    private final static String TAG = "LoadTask";
    private AndroidCloudAPI mApi;
    private Class<?> mModel;

    public LoadTask(AndroidCloudAPI api, Class<?> model) {
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
                if (mApi instanceof SoundCloudApplication) {
                    CloudUtils.resolveParcelable((Context) mApi, model, ((SoundCloudApplication) mApi).getCurrentUserId());
                }
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

     public static class LoadUserTask extends LoadTask<User> {
        public LoadUserTask(AndroidCloudAPI api) {
            super(api, User.class);
        }
    }
}
