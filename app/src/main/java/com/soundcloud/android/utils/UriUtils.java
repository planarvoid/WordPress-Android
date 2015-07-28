package com.soundcloud.android.utils;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.java.collections.ListMultiMap;
import com.soundcloud.java.collections.MultiMap;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class UriUtils {
    public static long getLastSegmentAsLong(Uri uri) {
        try {
            return Long.parseLong(uri.getLastPathSegment());
        } catch (NumberFormatException e) {
            if (Log.isLoggable(SoundCloudApplication.TAG, Log.DEBUG)) {
                Log.d(SoundCloudApplication.TAG, "Could not parse last segment as long from URI " + uri.toString());
            }
        }
        return -1l;
    }

    /**
     * Remove query params from Uri
     */
    public static Uri clearQueryParams(Uri contentUri) {
        // could use clearQuery here but its min api 11
        return TextUtils.isEmpty(contentUri.getQuery()) ? contentUri : contentUri.buildUpon().query(null).build();
    }

    public static MultiMap<String, String> getQueryParameters(String uriString) {
        return TextUtils.isEmpty(uriString) ? new ListMultiMap<String, String>() : getQueryParameters(Uri.parse(uriString));
    }

    public static MultiMap<String, String> getQueryParameters(Uri uri) {
        MultiMap<String, String> params = new ListMultiMap<>();
        for (String key : getQueryParameterNames(uri)) {
            params.putAll(key, uri.getQueryParameters(key));
        }
        return params;
    }

    public static String getPathWithQuery(Uri uri) {
        return uri.getPath() + (uri.getQuery() != null ? ("?" + uri.getQuery()) : "");
    }

    /**
     * Returns a set of the unique names of all query parameters. Iterating
     * over the set will return the names in order of their first occurrence.
     *
     * http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/4.1.1_r1/android/net/Uri.java?av=f#1554
     *
     * @return a set of decoded names
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     */
    private static Set<String> getQueryParameterNames(Uri uri) {
        if (uri.isOpaque()) {
            throw new UnsupportedOperationException("This isn't a hierarchical URI.");
        }

        String query = uri.getEncodedQuery();
        if (query == null) {
            return Collections.emptySet();
        }

        Set<String> names = new LinkedHashSet<>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);
            names.add(Uri.decode(name));

            // Move start to end of name.
            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableSet(names);
    }

    private UriUtils() {
    }
}
