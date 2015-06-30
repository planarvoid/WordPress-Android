package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.api.legacy.Request;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

public final class HttpUtils {
    private HttpUtils() {
    }

    public static
    @Nullable
    Uri getRedirectUri(@NotNull HttpClient client, @NotNull Uri target) {
        try {
            HttpGet get = new HttpGet(target.toString());
            HttpResponse resp = client.execute(get);
            if (HttpStatus.SC_MOVED_TEMPORARILY == resp.getStatusLine().getStatusCode()) {
                Header location = resp.getFirstHeader("Location");
                if (location != null && !TextUtils.isEmpty(location.getValue())) {
                    return Uri.parse(location.getValue());
                }
            } else {
                Log.w(TAG, "invalid status " + resp.getStatusLine());
            }
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return null;
    }

    /**
     * Adds an optional list of query params to the given request object.
     *
     * @param request the SoundCloud API request
     * @param params  null, empty array, or key-value pairs
     */
    public static Request addQueryParams(Request request, String... params) {
        if (params != null) {
            if (params.length % 2 != 0) {
                throw new IllegalArgumentException("Query params must be passed in k/v pairs");
            }
            for (int i = 0; i < params.length; i += 2) {
                request.add(params[i], params[i + 1]);
            }
        }
        return request;
    }
}
