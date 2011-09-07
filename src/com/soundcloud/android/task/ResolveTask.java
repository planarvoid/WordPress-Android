package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.net.Uri;
import android.util.Log;

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
                    listener.onUrlResolved(Uri.parse(location.getValue()));
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
        void onUrlResolved(Uri uri);
        void onUrlError();
    }

    public static Uri resolveSoundCloudURI(Uri uri, ResolveListener listener) {
        Uri resolved = resolveSoundCloudURI(uri);
        if (listener != null) {
            if (resolved != null) {
                listener.onUrlResolved(resolved);
            } else {
                listener.onUrlError();
            }
        }
        return resolved;
    }

    public static Uri resolveSoundCloudURI(Uri uri) {
        if (uri != null && "soundcloud".equalsIgnoreCase(uri.getScheme())) {
            final String specific = uri.getSchemeSpecificPart();
            final String[] components = specific.split("/", 2);
            if (components != null && components.length == 2) {
                final String type = components[0];
                final String id = components[1];

                return new Uri.Builder()
                    .scheme("https")
                    .authority("api.soundcloud.com")
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
