package com.soundcloud.android.events;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;

public final class CurrentUserChangedEvent {

    public static final int USER_UPDATED = 0;
    public static final int USER_REMOVED = 1;

    private final int kind;
    @Nullable
    private final PropertySet currentUser;

    public static CurrentUserChangedEvent forLogout() {
        return new CurrentUserChangedEvent(USER_REMOVED, null);
    }

    public static CurrentUserChangedEvent forUserUpdated(final PublicApiUser currentUser) {
        return new CurrentUserChangedEvent(USER_UPDATED, currentUser.toPropertySet());
    }

    private CurrentUserChangedEvent(int kind, @Nullable PropertySet user) {
        this.kind = kind;
        currentUser = user;
    }

    public int getKind() {
        return kind;
    }

    @Nullable
    public PropertySet getCurrentUser() {
        return currentUser;
    }
}
