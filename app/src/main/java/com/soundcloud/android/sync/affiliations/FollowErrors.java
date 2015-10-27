package com.soundcloud.android.sync.affiliations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class FollowErrors {

    private final List<FollowError> errors;

    public FollowErrors(@JsonProperty("errors") List<FollowError> errors) {
        this.errors = errors;
    }

    public static FollowErrors empty() {
        return new FollowErrors(Collections.<FollowError>emptyList());
    }

    public boolean isAgeRestricted() {
        for (FollowError error : errors) {
            if (error.isAgeRestricted()) {
                return true;
            }
        }
        return false;
    }

    public boolean isAgeUnknown() {
        for (FollowError error : errors) {
            if (error.isAgeUnknown()) {
                return true;
            }
        }
        return false;
    }

    public @Nullable Integer getAge() {
        for (FollowError error : errors) {
            if (error.age != null) {
                return error.age;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ApiErrors{" +
                "errors=" + errors +
                '}';
    }
}
