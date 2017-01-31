package com.soundcloud.android.onboarding.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import android.support.annotation.Nullable;

@AutoValue
public abstract class SignUpBody {
    @JsonProperty("client_id") abstract String clientId();
    @JsonProperty("client_secret") abstract String clientSecret();
    @JsonProperty("email_address") abstract String email();
    @JsonProperty("password") abstract String password();
    @JsonProperty("gender") @Nullable abstract String gender();
    @JsonProperty("dob") @Nullable abstract DateOfBirth dateOfBirth();

    public static SignUpBody create(String clientId, String clientSecret, String email, String password, @Nullable String gender, long year, long month) {
        return new AutoValue_SignUpBody(clientId, clientSecret, email, password, gender, DateOfBirth.create(year, month));
    }

}
