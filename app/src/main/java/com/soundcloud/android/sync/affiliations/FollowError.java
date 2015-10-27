package com.soundcloud.android.sync.affiliations;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FollowError {

    private static final String AGE_RESTRICTED = "DENY_AGE_RESTRICTED";
    private static final String AGE_UNKNOWN = "DENY_AGE_UNKNOWN";

    public final String errorMessage;
    public final Integer age;

    public FollowError(@JsonProperty("error_message") String errorMessage, @JsonProperty("age") Integer age) {
        this.errorMessage = errorMessage;
        this.age = age;
    }

    public boolean isAgeRestricted() {
        return AGE_RESTRICTED.equals(errorMessage);
    }

    public boolean isAgeUnknown() {
        return AGE_UNKNOWN.equals(errorMessage);
    }

    @Override
    public String toString() {
        return "ApiError{" +
                "errorMessage='" + errorMessage + '\'' +
                ", age=" + age +
                '}';
    }
}
