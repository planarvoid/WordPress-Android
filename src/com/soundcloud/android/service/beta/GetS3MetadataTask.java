package com.soundcloud.android.service.beta;

import static com.soundcloud.android.service.beta.BetaService.TAG;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

public class GetS3MetadataTask extends AsyncTask<Content, Void, Content> {
    public static final String HEADER_PREFIX = "x-amz-meta-";
    private HttpClient mClient;

    public GetS3MetadataTask(HttpClient client) {
        mClient = client;
    }

    @Override
    protected Content doInBackground(Content... params) {
        final Content key = params[0];

        HttpUriRequest request = new HttpHead(key.getURI().toString());
        try {
            HttpResponse resp = mClient.execute(request);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                for (Header h : resp.getAllHeaders()) {
                    if (h.getName().startsWith(HEADER_PREFIX)) {
                        String hKey = h.getName().substring(HEADER_PREFIX.length(), h.getName().length());
                        key.metadata.put(hKey, h.getValue());
                    }
                }
                return key;
            } else {
                Log.w(TAG, "unexpected response: "+resp.getStatusLine());
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, "error retrieving metadata", e);
            return null;
        }
    }
}
