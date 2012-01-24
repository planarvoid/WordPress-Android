package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Parcelable;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class ResolveTask extends AsyncApiTask<Uri, Void, HttpResponse>  {
    private WeakReference<ResolveListener> mListener;

    public ResolveTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected HttpResponse doInBackground(Uri... params) {
        final Uri uri = params[0];
        try {
            return mApi.get(Request.to(Endpoints.RESOLVE).add("url",uri.toString()));
        } catch (IOException e) {
            warn("error resolving url", e);
            return null;
        }
    }

    public void setListener(ResolveListener listener){
        mListener = new WeakReference<ResolveListener>(listener);
    }

    @Override
    protected void onPostExecute(HttpResponse response) {
        ResolveListener listener = mListener != null ? mListener.get() : null;
        if (listener == null) return;
        if (response != null) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                final Header location = response.getFirstHeader("Location");
                if (location != null && location.getValue() != null) {
                    listener.onUrlResolved(Uri.parse(location.getValue()), null);
                } else {
                    listener.onUrlError();
                }
            } else {
                warn("unexpected status code: "+response.getStatusLine());
                listener.onUrlError();
            }
        } else {
            listener.onUrlError();
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

    public static ScModel resolveLocally(ContentResolver resolver, Uri uri) {
        if (uri != null && "soundcloud".equalsIgnoreCase(uri.getScheme())) {
            final String specific = uri.getSchemeSpecificPart();
            final String[] components = specific.split(":", 2);
            if (components != null && components.length == 2) {
                final String type = components[0];
                final String id = components[1];

                if (type != null && id != null) {
                    try {
                        long _id = Long.parseLong(id);
                        if ("tracks".equalsIgnoreCase(type)) {
                            return SoundCloudDB.getTrackById(resolver, _id);
                        } else if ("users".equalsIgnoreCase(type)) {
                            return SoundCloudDB.getUserById(resolver, _id);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }
}
