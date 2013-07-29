package com.soundcloud.android.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.soundcloud.android.SoundCloudApplication;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class UriUtils {
    public static long getLastSegmentAsLong(Uri uri){
        try {
            return Long.parseLong(uri.getLastPathSegment());
        } catch (NumberFormatException e){
            if (Log.isLoggable(SoundCloudApplication.TAG,Log.DEBUG)){
                Log.d(SoundCloudApplication.TAG,"Could not parse last segment as long from URI " + uri.toString());
            }
        }
        return -1l;
    }

    /**
     * Remove query params from Uri
     * @param contentUri
     * @return
     */
    public static Uri clearQueryParams(Uri contentUri) {
        // could use clearQuery here but its min api 11
        return TextUtils.isEmpty(contentUri.getQuery()) ? contentUri : contentUri.buildUpon().query(null).build();
    }

    public static Multimap<String, String> getQueryParameters(String uriString){
        return getQueryParameters(Uri.parse(uriString));
    }

    public static Multimap<String, String> getQueryParameters(Uri uri){
        Multimap<String,String> params = ArrayListMultimap.create();
        for (String key : getQueryParameterNames(uri)){
            params.get(key).addAll(uri.getQueryParameters(key));
        }
        return params;
    }

    /**
     * Returns a set of the unique names of all query parameters. Iterating
     * over the set will return the names in order of their first occurrence.
     *
     * http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/4.1.1_r1/android/net/Uri.java?av=f#1554
     *
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     *
     * @return a set of decoded names
     */
    private static Set<String> getQueryParameterNames(Uri uri) {
        if (uri.isOpaque()) {
            throw new UnsupportedOperationException("This isn't a hierarchical URI.");
        }

        String query = uri.getEncodedQuery();
        if (query == null) {
            return Collections.emptySet();
        }

        Set<String> names = new LinkedHashSet<String>();
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
}
