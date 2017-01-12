package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.User;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class UserChangedEvent {
    public abstract Map<Urn, User> changeMap();

    public static UserChangedEvent forUpdate(User user) {
        return new AutoValue_UserChangedEvent(Collections.singletonMap(user.urn(), user));
    }

    public static UserChangedEvent forUpdate(Collection<User> users) {
        final Map<Urn, User> changeSet = new HashMap<>(users.size());
        for (User user : users) {
            changeSet.put(user.urn(), user);
        }
        return new AutoValue_UserChangedEvent(changeSet);
    }
}
