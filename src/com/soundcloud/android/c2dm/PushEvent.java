package com.soundcloud.android.c2dm;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.soundcloud.android.service.sync.SyncAdapterService;

public enum PushEvent {
    LIKE("like"),
    FOLLOWER("follower"),
    COMMENT("comment"),
    UNKNOWN("unknown"),
    NULL(null);

    public final String type;
    public String uri;

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
                    if (extras.containsKey(SyncAdapterService.EXTRA_PUSH_EVENT_URI)){
                        e.uri = extras.getString(SyncAdapterService.EXTRA_PUSH_EVENT_URI);
                    } else if (extras.containsKey(C2DMReceiver.SC_URI)){
                        e.uri = extras.getString(C2DMReceiver.SC_URI);
                    }
                    return e;
                }
            }
            return UNKNOWN;
        } else {
            return NULL;
        }
    }
}
