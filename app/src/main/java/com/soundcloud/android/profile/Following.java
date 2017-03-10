package com.soundcloud.android.profile;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserAssociation;

@AutoValue
public abstract class Following {
    public abstract User user();

    public abstract UserAssociation userAssociation();

    public static Following from(User user, UserAssociation userAssociation) {
        return new AutoValue_Following(user, userAssociation);
    }
}
