package com.soundcloud.android.deeplinks;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.storage.BulkStorage;
import com.soundcloud.android.tasks.FetchModelTask;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;

public class ResolveFetchTask extends AsyncTask<Uri, Void, PublicApiResource> {
    private static final String TAG = ResolveFetchTask.class.getSimpleName();
    private final ContentResolver contentResolver;

    private WeakReference<FetchModelTask.Listener<PublicApiResource>> listener;
    private final PublicCloudAPI api;
    private final ApiClient apiClient;
    private Uri unresolvedUrl;

    public ResolveFetchTask(PublicCloudAPI api, ContentResolver contentResolver, ApiClient apiClient) {
        this.api = api;
        this.apiClient = apiClient;
        this.contentResolver = contentResolver;
    }

    @Override
    protected PublicApiResource doInBackground(Uri... params) {
        Uri uri = fixUri(params[0]);

        // first resolve any click tracking urls
        if (isClickTrackingUrl(uri)) {
            Uri resolved = HttpUtils.getRedirectUri(api.getHttpClient(), uri);
            if (resolved == null) {
                resolved = extractClickTrackingRedirectUrl(uri);
            }
            uri = resolved;
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "resolving uri " + uri + " remotely");
        }
        Uri resolvedUri = new ResolveTask(api).resolve(uri);
        if (resolvedUri != null) {
            try {
                return fetchResource(resolvedUri);
            } catch (IllegalArgumentException e) {
                unresolvedUrl = uri;
                return null;
            }
        } else {
            unresolvedUrl = uri;
            return null;
        }
    }

    private PublicApiResource fetchResource(Uri resolvedUri) {
        final ApiRequest request = ApiRequest.get(resolvedUri.getPath() + (resolvedUri.getQuery() != null ? ("?" + resolvedUri.getQuery()) : ""))
                .forPublicApi()
                .build();

        return new FetchModelTask<PublicApiResource>(apiClient) {
            @Override
            protected void persist(PublicApiResource scResource) {
                new BulkStorage(contentResolver).bulkInsert(Lists.newArrayList(scResource));
            }
        }.resolve(request);
    }

    @Override
    protected void onPostExecute(PublicApiResource resource) {
        FetchModelTask.Listener<PublicApiResource> listener = getListener();
        if (listener != null) {
            if (resource != null) {
                listener.onSuccess(resource);
            } else {
                listener.onError(unresolvedUrl);
            }
        }
    }

    public void setListener(FetchModelTask.Listener<PublicApiResource> listener) {
        this.listener = new WeakReference<>(listener);
    }

    private FetchModelTask.Listener<PublicApiResource> getListener() {
        return listener == null ? null : listener.get();
    }

    static boolean isClickTrackingUrl(Uri uri) {
        return "soundcloud.com".equals(uri.getHost()) && uri.getPath().startsWith("/-/t/click");
    }

    static
    @NotNull
    Uri extractClickTrackingRedirectUrl(Uri uri) {
        if (isClickTrackingUrl(uri)) {
            String url = uri.getQueryParameter("url");
            if (!TextUtils.isEmpty(url)) {
                return Uri.parse(url);
            }
        }
        return uri;
    }

    //only handle the first 3 path segments (resource only for now, actions to be implemented later)
    /* package */
    static Uri fixUri(Uri data) {
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
}
