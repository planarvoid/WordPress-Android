package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
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

public class ResolveTask extends AsyncApiTask<Uri, Void, Uri>  {
    private WeakReference<ResolveListener> mListener;

    public ResolveTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected Uri doInBackground(Uri... params) {
        final Uri uri = params[0];
        Uri local = resolveSoundCloudURI(uri, mApi.getEnv());
        if (local != null) {
            return local;
        }

        try {
            HttpResponse resp = mApi.get(Request.to(Endpoints.RESOLVE).add("url",uri.toString()));

            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                final Header location = resp.getFirstHeader("Location");
                if (location != null && location.getValue() != null) {
                    return Uri.parse(location.getValue());
                } else {
                    return null;
                }
            } else {
                warn("unexpected status code: "+resp.getStatusLine());
                return null;
            }
        } catch (IOException e) {
            warn("error resolving url", e);
            return null;
        }
    }

    public void setListener(ResolveListener listener){
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

    protected void onUrlError() {
        ResolveListener listener = mListener != null ? mListener.get() : null;
        if (listener != null) {
            listener.onUrlError();
        }
    }

    protected void onUrlResolved(Uri url, @Nullable String action) {
        ResolveListener listener = mListener != null ? mListener.get() : null;
        if (listener != null) {
            listener.onUrlResolved(url, action);
        }
    }

    public interface ResolveListener {
        void onUrlResolved(Uri uri, String action);
        void onUrlError();
    }

    public static Uri resolveSoundCloudURI(Uri uri, Env env, ResolveListener listener) {
        Uri resolved = resolveSoundCloudURI(uri, env);
        if (listener != null) {
            if (resolved != null) {
                listener.onUrlResolved(resolved, uri.getFragment());
            } else {
                listener.onUrlError();
            }
        }
        return resolved;
    }

    // http://soundcloud.pbworks.com/w/page/40109213/Client%20URL%20Scheme
    public static Uri resolveSoundCloudURI(Uri uri, Env env) {
        if (uri != null && "soundcloud".equalsIgnoreCase(uri.getScheme())) {
            final String specific = uri.getSchemeSpecificPart();
            final String[] components = specific.split(":", 2);
            if (components != null && components.length == 2) {
                final String type = components[0];
                final String id = components[1];

                return new Uri.Builder()
                    .scheme(env.sslResourceHost.getSchemeName())
                    .authority(env.sslResourceHost.getHostName())
                    .appendPath(type)
                    .appendPath(id).build();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


}
