package com.soundcloud.android.onboarding.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class SignInBody {
    private static final String AUTH_TYPE_PASSWORD = "password";
    private static final String AUTH_TYPE_FACEBOOK = "facebook";
    private static final String AUTH_TYPE_GOOGLE = "google";

    @JsonProperty("credentials") public abstract Credentials credentials();
    @JsonProperty("auth_method") abstract String authMethod();
    @JsonProperty("client_id") abstract String clientId();
    @JsonProperty("client_secret") abstract String clientSecret();
    @JsonProperty("create_if_not_found") abstract boolean createIfNotFound();
    @JsonProperty("signature") abstract Optional<String> signature();

    private static SignInBody withCredentials(Credentials credentials, String authMethod, String clientId, String clientSecret) {
        return new AutoValue_SignInBody(credentials, authMethod, clientId, clientSecret, true, Optional.absent());
    }

    public static SignInBody withUserCredentials(String identifier, String password, String clientId, String clientSecret, String signature) {
        return new AutoValue_SignInBody(UserCredentials.create(identifier, password), AUTH_TYPE_PASSWORD, clientId, clientSecret, true, Optional.of(signature));
    }

    public static SignInBody withFacebookToken(String token, String clientId, String clientSecret) {
        return withCredentials(FacebookToken.create(token), AUTH_TYPE_FACEBOOK, clientId, clientSecret);
    }

    public static SignInBody withGoogleToken(String token, String clientId, String clientSecret) {
        return withCredentials(GoogleToken.create(token), AUTH_TYPE_GOOGLE, clientId, clientSecret);
    }
}
