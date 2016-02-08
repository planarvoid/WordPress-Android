package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.objects.MoreObjects;

@AutoValue
public abstract class StreamEvent {

    public static final int STREAM_REFRESHED = 0;

    public static StreamEvent fromStreamRefresh() {
        return new AutoValue_StreamEvent(STREAM_REFRESHED);
    }

    public abstract int getKind();

    public boolean isNewItemsEvent() {
        return getKind() == STREAM_REFRESHED;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("kind", getKind()).toString();
    }

}
