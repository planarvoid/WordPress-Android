package com.soundcloud.android.profile;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.users.UserItem;

@AutoValue
public abstract class Following {
    public abstract UserItem userItem();

    public abstract UserAssociation userAssociation();

    public static Following from(UserItem userItem, UserAssociation userAssociation) {
        return new AutoValue_Following(userItem, userAssociation);
    }
}
