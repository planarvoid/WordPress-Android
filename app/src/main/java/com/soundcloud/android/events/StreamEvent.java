package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class StreamEvent {

    public static final int STREAM_REFRESHED = 0;

    public static StreamEvent fromStreamRefresh() {
        return new AutoValue_StreamEvent(STREAM_REFRESHED);
    }

    public abstract int getKind();

    public boolean isStreamRefreshed() {
        return getKind() == STREAM_REFRESHED;
    }

}
