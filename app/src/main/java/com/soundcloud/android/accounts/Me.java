package com.soundcloud.android.accounts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.users.UserRecord;

import android.support.annotation.Nullable;

@AutoValue
public abstract class Me {

    @JsonCreator
    public static Me create(@JsonProperty("user") @Nullable PublicApiUser user, @JsonProperty("configuration") @Nullable Configuration configuration, @JsonProperty("primary_email_confirmed") boolean primaryEmailConfirmed) {
        return createFromUserRecord(user, configuration, primaryEmailConfirmed);
    }

    public static Me createFromUserRecord(@Nullable UserRecord user,Configuration configuration,boolean primaryEmailConfirmed) {
        return new AutoValue_Me(
                user, configuration, primaryEmailConfirmed
        );
    }

    public abstract UserRecord getUser();
    // @Nullable can be removed once the AddUserInfoTask is reworked to use api-mobile
    @Nullable public abstract Configuration getConfiguration();

    public abstract boolean isPrimaryEmailConfirmed();
}
