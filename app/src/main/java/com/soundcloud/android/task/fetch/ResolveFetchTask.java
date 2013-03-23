package com.soundcloud.android.task.fetch;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.task.ResolveTask;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;

public class ResolveFetchTask extends AsyncTask<Uri, Void, ScResource> {
    private static final String TAG = ResolveFetchTask.class.getSimpleName();

    private WeakReference<FetchModelTask.Listener<ScResource>> mListener;
    private AndroidCloudAPI mApi;
    private Uri mUnresolvedUrl;

    public ResolveFetchTask(AndroidCloudAPI api) {
        mApi = api;
    }

    @Override
    protected ScResource doInBackground(Uri... params) {
        Uri uri = fixUri(params[0]);

        // first resolve any click tracking urls
        if (isClickTrackingUrl(uri)) {
            Uri resolved = HttpUtils.getRedirectUri(mApi.getHttpClient(), uri);
            if (resolved == null) {
                resolved = extractClickTrackingRedirectUrl(uri);
            }
            uri = resolved;
        }

        ScResource resource = resolveLocally(uri);
        if (resource != null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "resolved uri "+uri+" locally");
            return resource;
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "resolving uri "+uri+" remotely");

        Uri resolvedUri = new ResolveTask(mApi).resolve(uri);
        if (resolvedUri != null) {
            final Request request = Request.to(resolvedUri.getPath() +
                (resolvedUri.getQuery() != null ? ("?"+resolvedUri.getQuery()) : ""));

            return new FetchModelTask(mApi).resolve(request);
        } else {
            mUnresolvedUrl = uri;
            return null;
        }
    }

    @Override
    protected void onPostExecute(ScResource resource) {
        FetchModelTask.Listener<ScResource> listener = getListener();
        if (listener != null) {
            if (resource != null) {
                listener.onSuccess(resource);
            } else {
                listener.onError(mUnresolvedUrl);
            }
        }
    }

    public void setListener(FetchModelTask.Listener<ScResource> listener) {
        mListener = new WeakReference<FetchModelTask.Listener<ScResource>>(listener);
    }

    private FetchModelTask.Listener<ScResource> getListener() {
        return mListener == null ? null : mListener.get();
    }

    static boolean isClickTrackingUrl(Uri uri) {
        return "soundcloud.com".equals(uri.getHost()) && uri.getPath().startsWith("/-/t/click");
    }

    static @NotNull Uri extractClickTrackingRedirectUrl(Uri uri) {
        if (isClickTrackingUrl(uri)) {
            String url = uri.getQueryParameter("url");
            if (!TextUtils.isEmpty(url)) {
                return Uri.parse(url);
            }
        }
        return uri;
    }

    //only handle the first 3 path segments (resource only for now, actions to be implemented later)
    /* package */ static Uri fixUri(Uri data) {
        if (!data.getPathSegments().isEmpty()) {
            final int cutoff;
            final int segments = data.getPathSegments().size();
            if (segments > 1 &&
                    ("follow".equals(data.getPathSegments().get(1)) ||
                     "favorite".equals(data.getPathSegments().get(1)))) {
                cutoff = 1;
            } else {
                cutoff = segments;
            }
            if (cutoff > 0) {
                return data.buildUpon().path(TextUtils.join("/", data.getPathSegments().subList(0, cutoff))).build();
            }
        }
        return data;
    }

    private ScResource resolveLocally(Uri uri) {
        if (uri != null && ClientUri.SCHEME.equalsIgnoreCase(uri.getScheme())) {
            final String specific = uri.getSchemeSpecificPart();
            final String[] components = specific.split(":", 2);
            if (components != null && components.length == 2) {
                final String type = components[0];
                final String id = components[1];

                if (type != null && id != null) {
                    try {
                        long _id = Long.parseLong(id);
                        if (ClientUri.TRACKS_TYPE.equalsIgnoreCase(type)) {
                            return SoundCloudApplication.MODEL_MANAGER.getTrack(_id);
                        } else if (ClientUri.PLAYLISTS_TYPE.equalsIgnoreCase(type)) {
                            return SoundCloudApplication.MODEL_MANAGER.getPlaylist(_id);
                        } else if (ClientUri.USERS_TYPE.equalsIgnoreCase(type)) {
                            return SoundCloudApplication.MODEL_MANAGER.getUser(_id);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }
}
