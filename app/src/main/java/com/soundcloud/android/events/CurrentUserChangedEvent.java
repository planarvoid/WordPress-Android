package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.Nullable;

public final class CurrentUserChangedEvent {

    public static final int USER_UPDATED = 0;
    public static final int USER_REMOVED = 1;

    private final int kind;
    @Nullable
    private final Urn currentUserUrn;

    public static CurrentUserChangedEvent forLogout() {
        return new CurrentUserChangedEvent(USER_REMOVED, Urn.NOT_SET);
    }

    public static CurrentUserChangedEvent forUserUpdated(final Urn currentUserUrn) {
        return new CurrentUserChangedEvent(USER_UPDATED, currentUserUrn);
    }

    private CurrentUserChangedEvent(int kind, Urn currentUserUrn) {
        this.kind = kind;
        this.currentUserUrn = currentUserUrn;
    }

    public int getKind() {
        return kind;
    }

    @Nullable
    public Urn getCurrentUserUrn() {
        return currentUserUrn;
    }
}
