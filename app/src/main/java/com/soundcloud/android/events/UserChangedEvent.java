package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserItem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class UserChangedEvent {
    public abstract Map<Urn, UserItem> changeMap();

    public static UserChangedEvent forUpdate(UserItem userItem) {
        return new AutoValue_UserChangedEvent(Collections.singletonMap(userItem.getUrn(), userItem));
    }

    public static UserChangedEvent forUpdate(Collection<UserItem> userItems) {
        final Map<Urn, UserItem> changeSet = new HashMap<>(userItems.size());
        for (UserItem userItem : userItems) {
            changeSet.put(userItem.getUrn(), userItem);
        }
        return new AutoValue_UserChangedEvent(changeSet);
    }
}
