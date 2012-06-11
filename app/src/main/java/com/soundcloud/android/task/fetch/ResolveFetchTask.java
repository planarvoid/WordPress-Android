package com.soundcloud.android.task.fetch;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.task.ResolveTask;
import com.soundcloud.api.Request;

import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ResolveFetchTask extends AsyncTask<Uri, Void, ScModel> {
    private static final String TAG = "ResolveFetchTask";
    private WeakReference<FetchModelTask.FetchModelListener<ScModel>> mListener;
    private AndroidCloudAPI mApi;

    public ResolveFetchTask(AndroidCloudAPI api) {
        mApi = api;
    }

    @Override
    protected ScModel doInBackground(Uri... params) {
        final Uri uri = fixUri(params[0]);
        ScModel model = resolveLocally(uri);
        if (model != null) {
            return model;
        }

        try {
            Uri resolvedUri = new ResolveTask(mApi).execute(uri).get();
            if (resolvedUri != null) {
                List<String> segments = resolvedUri.getPathSegments();
                if (segments.size() >= 2) {
                    FetchModelTask<?> task;
                    final String path = segments.get(0);
                    if ("tracks".equalsIgnoreCase(path)) {
                        task = new FetchTrackTask(mApi);
                    } else if ("users".equalsIgnoreCase(path)) {
                        task = new FetchUserTask(mApi);
                    } else {
                        return null;
                    }

                    final Request request = Request.to(resolvedUri.getPath() +
                            (resolvedUri.getQuery() != null ? ("?"+resolvedUri.getQuery()) : ""));

                    return task.execute(request).get();
                } else {
                    return null;
                }
            } else return null;
        } catch (InterruptedException e) {
            Log.w(TAG, e);
            return null;

        } catch (ExecutionException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(ScModel model) {
        FetchModelTask.FetchModelListener<ScModel> listener = getListener();
        if (listener != null) {
            if (model != null) {
                listener.onSuccess(model, null);
            } else {
                listener.onError(0);
            }
        }
    }

    public void setListener(FetchModelTask.FetchModelListener<ScModel> listener) {
        mListener = new WeakReference<FetchModelTask.FetchModelListener<ScModel>>(listener);
    }

    private FetchModelTask.FetchModelListener<ScModel> getListener() {
        return mListener == null ? null : mListener.get();
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

    private ScModel resolveLocally(Uri uri) {
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
                            return SoundCloudDB.getTrackById(mApi.getContext().getContentResolver(), _id);
                        } else if ("users".equalsIgnoreCase(type)) {
                            return SoundCloudDB.getUserById(mApi.getContext().getContentResolver(), _id);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }
}
