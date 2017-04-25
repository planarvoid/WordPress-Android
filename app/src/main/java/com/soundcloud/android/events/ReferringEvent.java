package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;

import android.os.Parcelable;

@AutoValue
public abstract class ReferringEvent implements Parcelable {
    public final static String REFERRING_EVENT_KEY = "referring_event_key";

    public static ReferringEvent create(String id, String kind) {
        return new AutoValue_ReferringEvent(id, kind);
    }

    public abstract String getId();

    public abstract String getKind();
}
