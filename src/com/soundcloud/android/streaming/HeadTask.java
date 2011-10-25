package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class HeadTask extends ApiTask {
    static final String LOG_TAG = HeadTask.class.getSimpleName();

    static private Map<String, ResolvedUrl> resolverCache = new HashMap<String, ResolvedUrl>();
    public static final long DEFAULT_URL_LIFETIME = 60 * 1000; // expire after 1 minute


    public HeadTask(StreamItem item, AndroidCloudAPI api) {
        super(item, api);
    }

    @Override
    public void handleResponse() {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            item.setContentLength(getContentLength(response));
            item.redirectedURL = getLocation(response);
        } else {
            Log.w(LOG_TAG, "invalid status received: " + response.getStatusLine());
        }
    }

    @Override
    protected HttpResponse performRequest() throws IOException {
        return api.get(Request.to(item.url));
    }

    private static String getLocation(HttpResponse resp) {
        Header h = resp.getFirstHeader("Location");
        return h != null ? h.getValue() : null;
    }

    private static long getContentLength(HttpResponse resp) {
        Header h = resp.getFirstHeader("Content-Length");
        if (h != null) {
            try {
                return Long.parseLong(h.getValue());
            } catch (NumberFormatException e) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    // TODO: use caching

    private synchronized String resolve(String streamUrl) throws IOException {
        ResolvedUrl resolved = resolverCache.get(streamUrl);
        if (resolved == null || resolved.isExpired()) {
            Log.d(LOG_TAG, "resolving url:" + streamUrl);
            resolved = new ResolvedUrl(doResolve(streamUrl));
            resolverCache.put(streamUrl, resolved);
        } else {
            Log.d(LOG_TAG, "using cached url:" + resolved);
        }
        return resolved.url;
    }


    private String doResolve(String streamUrl) throws IOException {
        HttpResponse resp = api.get(Request.to(streamUrl));
        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            final Header location = resp.getFirstHeader("Location");
            if (location != null && location.getValue() != null) {
                return location.getValue();
            } else {
                throw new IOException("no location header");
            }
        } else {
            throw new IOException("unexpected status code:" + resp.getStatusLine());
        }

    }

    static class ResolvedUrl {
        ResolvedUrl(String url) {
            this.url = url;
            String exp = Uri.parse(url).getQueryParameter("Expires");
            if (exp != null) {
                try {
                    this.expires = Long.parseLong(exp) * 1000L;
                    Log.d(LOG_TAG, String.format("url expires in %d secs", (this.expires-System.currentTimeMillis())/1000L));
                } catch (NumberFormatException e) {
                    this.expires = System.currentTimeMillis() + DEFAULT_URL_LIFETIME;
                }
            } else {
                this.expires = System.currentTimeMillis() + DEFAULT_URL_LIFETIME;
            }
        }

        long expires;
        final String url;

        public boolean isExpired() {
            return System.currentTimeMillis() > expires;
        }

        @Override
        public String toString() {
            return "ResolvedUrl{" +
                    "expires=" + expires +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}
