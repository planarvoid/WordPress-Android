package com.soundcloud.android.events;

import com.soundcloud.android.model.User;
import org.jetbrains.annotations.Nullable;

public final class CurrentUserChangedEvent {

    public static final int USER_UPDATED = 0;
    public static final int USER_REMOVED = 1;

    private final int kind;
    @Nullable
    private final User currentUser;

    public static CurrentUserChangedEvent forLogout() {
        return new CurrentUserChangedEvent(USER_REMOVED, null);
    }

    public static CurrentUserChangedEvent forUserUpdated(final User currentUser) {
        return new CurrentUserChangedEvent(USER_UPDATED, currentUser);
    }

    private CurrentUserChangedEvent(int kind, @Nullable User user) {
        this.kind = kind;
        currentUser = user;
    }

    public int getKind() {
        return kind;
    }

    @Nullable
    public User getCurrentUser() {
        return currentUser;
    }
}
