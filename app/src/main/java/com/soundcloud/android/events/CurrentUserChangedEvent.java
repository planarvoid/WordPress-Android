package com.soundcloud.android.events;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import org.jetbrains.annotations.Nullable;

public final class CurrentUserChangedEvent {

    public static final int USER_UPDATED = 0;
    public static final int USER_REMOVED = 1;

    private final int kind;
    @Nullable
    private final PublicApiUser currentUser;

    public static CurrentUserChangedEvent forLogout() {
        return new CurrentUserChangedEvent(USER_REMOVED, null);
    }

    public static CurrentUserChangedEvent forUserUpdated(final PublicApiUser currentUser) {
        return new CurrentUserChangedEvent(USER_UPDATED, currentUser);
    }

    private CurrentUserChangedEvent(int kind, @Nullable PublicApiUser user) {
        this.kind = kind;
        currentUser = user;
    }

    public int getKind() {
        return kind;
    }

    @Nullable
    public PublicApiUser getCurrentUser() {
        return currentUser;
    }
}
