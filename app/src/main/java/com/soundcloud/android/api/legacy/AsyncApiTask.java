package com.soundcloud.android.api.legacy;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.utils.IOUtils;

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
    protected final List<String> errors = new ArrayList<>();
    protected final PublicApiWrapper api;

    public AsyncApiTask(PublicApiWrapper api) {
        this.api = api;
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

    protected void extractErrors(HttpResponse resp) throws IOException {
        errors.addAll(IOUtils.parseError(api.getMapper().reader(), resp.getEntity().getContent()));
    }

    protected String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
}
