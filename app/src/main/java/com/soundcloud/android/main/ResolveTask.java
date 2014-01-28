package com.soundcloud.android.main;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.api.AsyncApiTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class ResolveTask extends AsyncApiTask<Uri, Void, Uri> {
    private WeakReference<ResolveListener> mListener;

    public ResolveTask(PublicCloudAPI api) {
        super(api);
    }

    @Override
    protected Uri doInBackground(Uri... params) {
        return resolve(params[0]);
    }

    public Uri resolve(Uri uri) {
        Uri local = resolveSoundCloudURI(uri, mApi.getEnv());
        if (local != null) {
            return local;
        }

        try {
            HttpResponse resp = mApi.get(Request.to(Endpoints.RESOLVE).add("url", uri.toString()));
            switch (resp.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_MOVED_TEMPORARILY: {
                    final Header location = resp.getFirstHeader("Location");
                    if (location != null && location.getValue() != null) {
                        return Uri.parse(location.getValue());
                    } else {
                        warn("no location header in response " + resp);
                        return null;
                    }
                }
                case HttpStatus.SC_NOT_FOUND: // item is gone
                    return null;

                default:
                    warn("unexpected status code: " + resp.getStatusLine());
                    return null;
            }
        } catch (IOException e) {
            warn("error resolving url", e);
            return null;
        }
    }

    public void setListener(ResolveListener listener) {
        mListener = new WeakReference<ResolveListener>(listener);
    }

    @Override
    protected void onPostExecute(Uri uri) {
        if (uri != null) {
            onUrlResolved(uri, null);
        } else {
            onUrlError();
        }
    }

    private void onUrlError() {
        ResolveListener listener = mListener != null ? mListener.get() : null;
        if (listener != null) {
            listener.onUrlError();
        }
    }

    private void onUrlResolved(Uri url, @Nullable String action) {
        ResolveListener listener = mListener != null ? mListener.get() : null;
        if (listener != null) {
            listener.onUrlResolved(url, action);
        }
    }

    public interface ResolveListener {
        void onUrlResolved(Uri uri, String action);
        void onUrlError();
    }

    @Nullable
    protected static Uri resolveSoundCloudURI(Uri uri, Env env) {
        try {
            Urn curi = Urn.parse(uri.toString());
            return new Uri.Builder()
                    .scheme(env.sslResourceHost.getSchemeName())
                    .authority(env.sslResourceHost.getHostName())
                    // handle api vs uri difference in tracks/sounds
                    .appendPath(curi.type.equalsIgnoreCase(Urn.SOUNDS_TYPE) ? Urn.TRACKS_TYPE : curi.type)
                    .appendPath(curi.id).build();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
