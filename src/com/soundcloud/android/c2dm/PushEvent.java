package com.soundcloud.android.c2dm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.soundcloud.android.view.UserlistRow;

public enum PushEvent {
    LIKE("like"),
    FOLLOWER("follower"),
    COMMENT("comment"),
    UNKNOWN("unknown"),
    NULL(null);

    public final String type;

    private PushEvent(String type) {
        this.type = type;
    }

    public static PushEvent fromIntent(Intent intent) {
        return intent == null ? NULL : fromExtras(intent.getExtras());
    }

    public static PushEvent fromExtras(Bundle extras) {
        if (extras == null) return NULL;

        String type = extras.getString(C2DMReceiver.SC_EXTRA_EVENT_TYPE);
        if (type == null) type = extras.getString(SyncAdapterService.EXTRA_PUSH_EVENT);
        if (!TextUtils.isEmpty(type)) {
            for (PushEvent e : PushEvent.values()) {
                if (type.equals(e.type)) {
                    return e;
                }
            }
            return UNKNOWN;
        } else {
            return NULL;
        }
    }

    public static long getIdFromUri(String uri) {
        return getIdFromUri(Uri.parse(uri));
    }

    public static long getIdFromUri(Uri uri) {
        if (uri != null && "soundcloud".equalsIgnoreCase(uri.getScheme())) {
            final String specific = uri.getSchemeSpecificPart();
            final String[] components = specific.split(":", 2);
            if (components != null && components.length == 2) {
                final String type = components[0];
                final String id = components[1];
                if (type != null && id != null) {
                    try {
                        return Long.parseLong(id);
                    } catch (NumberFormatException ignored) { }
                }
            }
        }
        return -1;
    }
}
