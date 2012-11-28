package com.soundcloud.android.model;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

public class ContentStats {
    private static Map<Content, Integer> sCounts = new HashMap<Content, Integer>();

    private static final Content[] CONTENTS = new Content[]{
            Content.ME_SOUND_STREAM,
            Content.ME_ACTIVITIES
    };

    public static final String NOTIFIED_ITEM = "notified.item";
    public static final String NOTIFIED = "notified";
    public static final String SEEN = "seen";

    /**
     * Asynchronously updates the unseen counters and broadcast change information to listeners.
     *
     * @param context the context.
     */
    public static synchronized void init(final Context context) {
        new AsyncTask<Content, Void, Void>() {
            @Override
            protected Void doInBackground(Content... contents) {
                for (Content content : contents) {
                    updateCount(context, content,
                            Activities.getCountSince(context.getContentResolver(), getLastSeen(context, content), content));
                }
                return null;
            }
        }.execute(CONTENTS);
    }

    public static long getLastSeen(Context context, Content content) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(prefKey(content, SEEN), 0);
    }

    public static long getLastNotified(Context context, Content content) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(prefKey(content, NOTIFIED), 0);
    }

    public static long getLastNotifiedItem(Context context, Content content) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(prefKey(content, NOTIFIED_ITEM), 0);
    }

    public static void setLastSeen(Context context, Content content, long timestamp) {
        setTimestamp(context, SEEN, content, timestamp);
    }

    public static void setLastNotified(Context context, Content content, long timestamp) {
        setTimestamp(context, NOTIFIED, content, timestamp);
    }

    public static void setLastNotifiedItem(Context context, Content content, long timestamp) {
        setTimestamp(context, NOTIFIED_ITEM, content, timestamp);
    }

    public static void setTimestamp(Context context, String key, Content content, long timestamp) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(prefKey(content, key), timestamp);
        SharedPreferencesUtils.apply(editor);
    }

    public static void updateCount(Context context, Content content, int count) {
        switch (content) {
            case ME_SOUND_STREAM:
            case ME_ACTIVITIES:
                if (count != count(content)) {
                    sCounts.put(content, count);
                    context.sendBroadcast(new Intent(Consts.GeneralIntents.ACTIVITIES_UNSEEN_CHANGED));
                }
                break;
        }
    }

    public static int count(Content content) {
        Integer count = sCounts.get(content);
        return count == null ? 0 : count;
    }

    private static String prefKey(Content content, String what) {
        return "last." + what + "." + content.uri.toString();
    }

    public static void clear(Context context) {
        for (Content c : CONTENTS) {
            setLastSeen(context, c, 1);
            setLastNotified(context, c, 1);
            setLastNotifiedItem(context, c, 1);
        }
    }

    public static void rewind(Context context, long time) {
        for (Content c : CONTENTS) {
            setLastSeen(context, c, Math.max(0, getLastSeen(context, c) - time));
            setLastNotified(context, c, Math.max(0, getLastNotified(context, c) - time));
            setLastNotifiedItem(context, c, Math.max(0, getLastNotifiedItem(context, c) - time));
        }
    }
}
