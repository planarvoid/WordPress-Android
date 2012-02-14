package com.soundcloud.android.c2dm;

import com.soundcloud.android.service.sync.SyncAdapterService;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

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
                if (type.equals(e.type)) return e;
            }
            return UNKNOWN;
        } else {
            return NULL;
        }
    }
}
