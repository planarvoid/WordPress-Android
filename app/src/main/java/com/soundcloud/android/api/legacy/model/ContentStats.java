package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.storage.provider.Content;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Inject;

public class ContentStats {

    public static final String NOTIFIED_ITEM = "notified.item";
    public static final String NOTIFIED = "notified";
    public static final String SEEN = "seen";
    private final Context context;

    @Inject
    public ContentStats(Context context) {
        this.context = context;
    }

    public long getLastSeen(Content content){
        return getLastSeen(context, content);
    }

    public void setLastSeen(Content content, long timestamp) {
        setLastSeen(context, content, timestamp);
    }

    public long getLastNotified(Content content) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(prefKey(content, NOTIFIED), 0);
    }

    public long getLastNotifiedItem(Content content){
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(prefKey(content, NOTIFIED_ITEM), 0);
    }

    public void setLastNotified(Content content, long timestamp) {
        setLastNotified(context, content, timestamp);
    }

    public void setLastNotifiedItem(Content content, long timestamp) {
        setTimestamp(context, NOTIFIED_ITEM, content, timestamp);
    }

    // Deprecated in favor of using non-static instances above

    @Deprecated
    public static long getLastSeen(Context context, Content content) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(prefKey(content, SEEN), 0);
    }

    @Deprecated
    public static void setLastSeen(Context context, Content content, long timestamp) {
        setTimestamp(context, SEEN, content, timestamp);
    }

    @Deprecated
    public static void setLastNotified(Context context, Content content, long timestamp) {
        setTimestamp(context, NOTIFIED, content, timestamp);
    }

    @Deprecated
    public static void setTimestamp(Context context, String key, Content content, long timestamp) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(prefKey(content, key), timestamp).apply();
    }

    private static String prefKey(Content content, String what) {
        return "last." + what + "." + content.uri.toString();
    }
}
