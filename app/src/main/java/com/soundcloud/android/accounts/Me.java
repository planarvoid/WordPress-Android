package com.soundcloud.android.accounts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.configuration.Configuration;

import android.support.annotation.Nullable;

@AutoValue
public abstract class Me {

    @JsonCreator
    public static Me create(@JsonProperty("user") @Nullable ApiUser apiUser, @JsonProperty("configuration") @Nullable Configuration configuration, @JsonProperty("primary_email_confirmed") boolean primaryEmailConfirmed) {
        return new AutoValue_Me(
                apiUser, configuration, primaryEmailConfirmed
        );
    }

    public abstract ApiUser getUser();
    // @Nullable can be removed once the AddUserInfoTask is reworked to use api-mobile
    @Nullable public abstract Configuration getConfiguration();

    public abstract boolean isPrimaryEmailConfirmed();
}
