package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.util.Log;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Endpoints;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;

import java.io.IOException;
import java.io.InputStream;
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
        mErrors.addAll(parseError(api().getMapper().reader(), resp.getEntity().getContent()));
    }

    static List<String> parseError(ObjectReader reader, InputStream is) throws IOException {
        List<String> errorList = new ArrayList<String>();
        JsonNode node = reader.readTree(is);
        //{"errors":{"error":["Email has already been taken","Email is already taken."]}}
        //{"errors":{"error":"Username has already been taken"}}
        //{"error":"Unknown Email Address"}
        //{"errors":[{"error_message":"Username is too short (minimum is 3 characters)"}]}
        JsonNode errors = node.path("errors").path("error");
        JsonNode error  = node.path("error");
        if (error.isTextual()) errorList.add(error.getTextValue());
        else if (errors.isTextual()) errorList.add(errors.getTextValue());
        else if (node.path("errors").isArray())
            for (JsonNode n : node.path("errors")) errorList.add(n.path("error_message").getTextValue());
        else for (JsonNode s : errors) errorList.add(s.getTextValue());

        return errorList;
    }

    protected String getFirstError() {
        return mErrors.isEmpty() ? null : mErrors.get(0);
    }
}
