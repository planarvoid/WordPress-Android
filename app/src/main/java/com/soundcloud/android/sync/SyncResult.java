package com.soundcloud.android.sync;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class SyncResult {

    enum Kind {
        SYNCED,
        SYNCING,
        NO_OP,
        ERROR
    }

    abstract Kind kind();

    public abstract Optional<Throwable> throwable();

    public boolean isError() {
        return kind().equals(Kind.ERROR);
    }

    public static SyncResult synced() {
        return new AutoValue_SyncResult(Kind.SYNCED, Optional.absent());
    }

    public static SyncResult syncing() {
        return new AutoValue_SyncResult(Kind.SYNCING, Optional.absent());
    }

    public static SyncResult noOp() {
        return new AutoValue_SyncResult(Kind.NO_OP, Optional.absent());
    }

    public static SyncResult error(Throwable throwable) {
        return new AutoValue_SyncResult(Kind.ERROR, Optional.of(throwable));
    }
}
