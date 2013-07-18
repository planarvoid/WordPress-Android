package com.soundcloud.android.utils;

import com.soundcloud.android.SoundCloudApplication;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

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
}
