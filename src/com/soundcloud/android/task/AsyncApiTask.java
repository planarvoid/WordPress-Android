package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.util.Log;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Endpoints;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public abstract class AsyncApiTask<Params, Progress, Result>
        extends AsyncTask<Params, Progress, Result>
        implements Endpoints, HttpStatus {
    protected List<String> mErrors = new ArrayList<String>();
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

    protected void warn(String s) {
        Log.w(TAG, s);
    }

    protected void extractErrors(HttpResponse resp) throws IOException {
        JsonNode node = api().getMapper().reader().readTree(resp.getEntity().getContent());
       //{"errors":{"error":["Email has already been taken","Email is already taken."]}}
       //{"errors":{"error":"Username has already been taken"}}
        JsonNode errors = node.path("errors").path("error");
        if (errors.isTextual()) mErrors.add(errors.getTextValue());
        for (JsonNode s : errors) {
            mErrors.add(s.getTextValue());
        }
    }

    protected String getFirstError() {
        return mErrors.isEmpty() ? null : mErrors.get(0);
    }
}
