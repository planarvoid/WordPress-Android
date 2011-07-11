package com.soundcloud.android.service.beta;


import static com.soundcloud.android.service.beta.BetaService.TAG;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class GetS3ContentTask extends AsyncTask<Uri, Void, List<Content>> {
    private HttpClient mClient;

    public GetS3ContentTask(HttpClient client) {
        mClient = client;
    }

    @Override
    protected List<Content> doInBackground(Uri... params) {
        try {
            HttpUriRequest req = new HttpGet(params[0].toString());
            req.setHeader("Accept", "text/xml");

            HttpResponse resp = mClient.execute(req);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return BucketParser.getContent(resp.getEntity().getContent());
            } else {
                Log.w(TAG, "unexpected status code:"+resp.getStatusLine());
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, "error", e);
            return null;
        }
    }
}
