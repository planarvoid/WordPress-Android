package com.soundcloud.android.accounts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiUser;

import android.support.annotation.Nullable;

@AutoValue
public abstract class Me {

    @JsonCreator
    public static Me create(@JsonProperty("user") @Nullable ApiUser apiUser) {
        return new AutoValue_Me(
                apiUser
        );
    }

    public abstract ApiUser getUser();
}
