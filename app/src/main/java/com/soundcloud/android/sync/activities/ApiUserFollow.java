package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiUser;

import java.util.Date;

public class ApiUserFollow {

    private final ApiUser user;
    private final Date createdAt;

    @JsonCreator
    public ApiUserFollow(@JsonProperty("user") ApiUser user,
                         @JsonProperty("created_at") Date createdAt) {
        this.user = user;
        this.createdAt = createdAt;
    }

    public ApiUser getUser() {
        return user;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
