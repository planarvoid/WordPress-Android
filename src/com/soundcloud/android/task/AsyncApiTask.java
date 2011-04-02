package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.util.Log;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.CloudAPI;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.lang.ref.WeakReference;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public abstract class AsyncApiTask<Params, Progress, Result>
        extends AsyncTask<Params, Progress, Result>
        implements CloudAPI.Enddpoints, HttpStatus {

    protected WeakReference<AndroidCloudAPI> mApi;

    public AsyncApiTask(AndroidCloudAPI api) {
        this.mApi = new WeakReference<AndroidCloudAPI>(api);
    }

    protected AndroidCloudAPI api() {
        return mApi.get();
    }

    public void warn(String s, HttpResponse response) {
        Log.w(TAG, s + ": " + response.getStatusLine().toString());
    }

    protected void warn(String s, Throwable throwable) {
        Log.w(TAG, s, throwable);
    }
}
