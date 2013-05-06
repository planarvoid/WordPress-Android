package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Endpoints;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AsyncApiTask<Params, Progress, Result>
        extends AsyncTask<Params, Progress, Result>
        implements Endpoints, HttpStatus {
    protected List<String> mErrors = new ArrayList<String>();
    protected AndroidCloudAPI mApi;

    public AsyncApiTask(AndroidCloudAPI api) {
        this.mApi = api;
    }

    public void warn(String s, HttpResponse response) {
        Log.w(TAG, s + ": " + response.getStatusLine().toString());
    }

    protected void warn(String s, Throwable throwable) {
        Log.w(TAG, s, throwable);
    }

    protected void warn(String s) {
        Log.w(TAG, s);
    }

    public List<String> getErrors() {
        return new ArrayList<String>(mErrors);
    }

    protected void extractErrors(HttpResponse resp) throws IOException {
        mErrors.addAll(IOUtils.parseError(mApi.getMapper().reader(), resp.getEntity().getContent()));
    }

    protected String getFirstError() {
        return mErrors.isEmpty() ? null : mErrors.get(0);
    }
}
