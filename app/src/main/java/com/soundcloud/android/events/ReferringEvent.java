package com.soundcloud.android.events;

import auto.parcel.AutoParcel;

import android.os.Parcelable;

@AutoParcel
public abstract class ReferringEvent implements Parcelable {
    public final static String REFERRING_EVENT_KEY = "referring_event_key";

    public static ReferringEvent create(String id, String kind) {
        return new AutoParcel_ReferringEvent(id, kind);
    }

    public abstract String getId();

    public abstract String getKind();
}
