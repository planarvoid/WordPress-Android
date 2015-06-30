package com.soundcloud.android.deeplinks;

import com.soundcloud.android.api.legacy.AsyncApiTask;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.api.legacy.Endpoints;
import com.soundcloud.android.api.legacy.Env;
import com.soundcloud.android.api.legacy.Request;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class ResolveTask extends AsyncApiTask<Uri, Void, Uri> {
    private WeakReference<ResolveListener> listener;

    public ResolveTask(PublicCloudAPI api) {
        super(api);
    }

    @Override
    protected Uri doInBackground(Uri... params) {
        return resolve(params[0]);
    }

    public Uri resolve(Uri uri) {
        Uri local = resolveSoundCloudURI(uri, api.getEnv());
        if (local != null) {
            return local;
        }

        try {
            HttpResponse resp = api.get(Request.to(Endpoints.RESOLVE).add("url", uri.toString()));
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
        this.listener = new WeakReference<>(listener);
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
        ResolveListener listener = this.listener != null ? this.listener.get() : null;
        if (listener != null) {
            listener.onUrlError();
        }
    }

    private void onUrlResolved(Uri url, @Nullable String action) {
        ResolveListener listener = this.listener != null ? this.listener.get() : null;
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
            UrnResolver resolver = new UrnResolver();
            Urn urn = resolver.toUrn(uri);

            String resourcePath;
            if (urn.isTrack()) {
                resourcePath = "tracks";
            } else if (urn.isPlaylist()) {
                resourcePath = "playlists";
            } else if (urn.isUser()) {
                resourcePath = "users";
            } else {
                return null;
            }
            return new Uri.Builder()
                    .scheme(env.getSecureResourceHost().getSchemeName())
                    .authority(env.getSecureResourceHost().getHostName())
                    .appendPath(resourcePath)
                    .appendPath(String.valueOf(urn.getNumericId())).build();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
