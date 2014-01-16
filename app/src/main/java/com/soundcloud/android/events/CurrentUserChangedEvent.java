package com.soundcloud.android.events;

import com.soundcloud.android.model.User;
import org.jetbrains.annotations.Nullable;

public class CurrentUserChangedEvent {

    public static final int USER_UPDATED = 0;
    public static final int USER_REMOVED = 1;

    private int mKind;
    @Nullable
    private User mCurrentUser;

    public static CurrentUserChangedEvent forLogout() {
        return new CurrentUserChangedEvent(USER_REMOVED, null);
    }

    public static CurrentUserChangedEvent forUserUpdated(final User currentUser) {
        return new CurrentUserChangedEvent(USER_UPDATED, currentUser);
    }

    private CurrentUserChangedEvent(int kind, @Nullable User user) {
        mKind = kind;
        mCurrentUser = user;
    }

    public int getKind() {
        return mKind;
    }

    @Nullable
    public User getCurrentUser() {
        return mCurrentUser;
    }
}
