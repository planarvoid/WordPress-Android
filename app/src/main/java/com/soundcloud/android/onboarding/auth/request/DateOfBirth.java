package com.soundcloud.android.onboarding.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class DateOfBirth {
    @JsonProperty("year") abstract long year();
    @JsonProperty("month") abstract long month();

    static DateOfBirth create(long year, long month) {
        return new AutoValue_DateOfBirth(year, month);
    }
}
