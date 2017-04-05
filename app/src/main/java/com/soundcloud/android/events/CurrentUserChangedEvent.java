package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class CurrentUserChangedEvent {

    private static final int USER_UPDATED = 0;
    private static final int USER_REMOVED = 1;

    public static CurrentUserChangedEvent forLogout() {
        return new AutoValue_CurrentUserChangedEvent(USER_REMOVED, Urn.NOT_SET);
    }

    public static CurrentUserChangedEvent forUserUpdated(final Urn currentUserUrn) {
        return new AutoValue_CurrentUserChangedEvent(USER_UPDATED, currentUserUrn);
    }

    abstract public int getKind();

    abstract public Urn getCurrentUserUrn();

    public boolean isUserRemoved() {
        return getKind() == USER_REMOVED;
    }

    public boolean isUserUpdated() {
        return getKind() == USER_UPDATED;
    }
}
