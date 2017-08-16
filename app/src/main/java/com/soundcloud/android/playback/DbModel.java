package com.soundcloud.android.playback;

import com.google.auto.value.AutoValue;

final class DbModel {

    @SuppressWarnings({"PMD.AbstractClassWithoutAnyMethod"})
    @AutoValue
    public abstract static class PlayQueue implements PlayQueueModel {
        public static final Factory<PlayQueue> FACTORY = new Factory<>(AutoValue_DbModel_PlayQueue::new);
    }
}
