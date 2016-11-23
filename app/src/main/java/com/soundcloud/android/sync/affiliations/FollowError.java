package com.soundcloud.android.sync.affiliations;

import com.fasterxml.jackson.annotation.JsonProperty;

class FollowError {

    private static final String AGE_RESTRICTED = "age_restricted";
    private static final String AGE_UNKNOWN = "age_unknown";

    public final String error;
    public final Integer age;

    public FollowError(@JsonProperty("error_key") String error, @JsonProperty("age") Integer age) {
        this.error = error;
        this.age = age;
    }

    boolean isAgeRestricted() {
        return AGE_RESTRICTED.equals(error);
    }

    boolean isAgeUnknown() {
        return AGE_UNKNOWN.equals(error);
    }

    @Override
    public String toString() {
        return "ApiError{" +
                "error='" + error + '\'' +
                ", age=" + age +
                '}';
    }
}
