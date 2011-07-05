package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.User;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

public abstract class LoadTask<Model extends Parcelable> extends AsyncTask<Request, Model, Model> {
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
        final ScActivity activity = mActivityReference.get();
        if (activity != null) {
            activity.setProgressBarIndeterminateVisibility(false);
        }
    }

    @Override
    protected Model doInBackground(Request... request) {
        if (request == null || request.length == 0) throw new IllegalArgumentException("need path to load");

        try {
            HttpResponse resp = mApi.get(request[0]);
            if (isCancelled()) return null;

            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Model model = mApi.getMapper().readValue(resp.getEntity().getContent(), mModel);
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
