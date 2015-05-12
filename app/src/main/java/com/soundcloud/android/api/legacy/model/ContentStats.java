package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.storage.provider.Content;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Inject;

public class ContentStats {

    private static final Content[] CONTENTS = new Content[] {
            Content.ME_SOUND_STREAM,
            Content.ME_ACTIVITIES
    };

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

    public long getLastNotifiedItem(Content content){
        return getLastNotifiedItem(context, content);
    }

    public void setLastNotified(Content content, long timestamp) {
        setLastNotified(context, content, timestamp);
    }

    public void setLastNotifiedItem(Content content, long timestamp) {
        setLastNotifiedItem(context, content, timestamp);
    }

    // Deprecated in favor of using non-static instances above

    @Deprecated
    public static long getLastSeen(Context context, Content content) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(prefKey(content, SEEN), 0);
    }

    @Deprecated
    public static long getLastNotified(Context context, Content content) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(prefKey(content, NOTIFIED), 0);
    }

    @Deprecated
    public static long getLastNotifiedItem(Context context, Content content) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(prefKey(content, NOTIFIED_ITEM), 0);
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
    public static void setLastNotifiedItem(Context context, Content content, long timestamp) {
        setTimestamp(context, NOTIFIED_ITEM, content, timestamp);
    }

    @Deprecated
    public static void setTimestamp(Context context, String key, Content content, long timestamp) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(prefKey(content, key), timestamp).apply();
    }

    @Deprecated
    public static void clear(Context context) {
        for (Content c : CONTENTS) {
            setLastSeen(context, c, 1);
            setLastNotified(context, c, 1);
            setLastNotifiedItem(context, c, 1);
        }
    }

    @Deprecated
    public static void rewind(Context context, long time) {
        for (Content c : CONTENTS) {
            setLastSeen(context, c, Math.max(0, getLastSeen(context, c) - time));
            setLastNotified(context, c, Math.max(0, getLastNotified(context, c) - time));
            setLastNotifiedItem(context, c, Math.max(0, getLastNotifiedItem(context, c) - time));
        }
    }

    private static String prefKey(Content content, String what) {
        return "last." + what + "." + content.uri.toString();
    }
}
