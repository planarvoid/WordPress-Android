package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.os.Parcelable;
import com.soundcloud.android.SoundCloudApplication;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

public abstract class JsonLoadTask<T> extends AsyncTask<HttpUriRequest, Parcelable, List<T>> {
    protected WeakReference<SoundCloudApplication> mApp;

    private SoundCloudApplication app;



    public JsonLoadTask(SoundCloudApplication app) {
        this.mApp = new WeakReference<SoundCloudApplication>(app);
    }

    protected InputStream httpGet(String path) throws IOException {
        return mApp.get().executeRequest(path);
    }
}
