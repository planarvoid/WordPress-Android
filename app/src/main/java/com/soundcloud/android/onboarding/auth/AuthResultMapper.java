package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.api.ApiRequestException.Reason.BAD_REQUEST;
import static com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult.validationError;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.onboarding.auth.tasks.AgeRestrictionAuthResult;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.io.IOException;

public class AuthResultMapper {
    @VisibleForTesting static final String INCORRECT_CREDENTIALS = "incorrect_credentials";
    @VisibleForTesting static final String EMAIL_TAKEN = "email_taken";
    @VisibleForTesting static final String SPAMMING = "spamming";
    @VisibleForTesting static final String AGE_RESTRICTED = "age_restricted";
    @VisibleForTesting static final String INVALID_EMAIL = "invalid_email";
    @VisibleForTesting static final String DOMAIN_BLACKLISTED = "domain_blacklisted";

    private final JsonTransformer jsonTransformer;

    @Inject
    AuthResultMapper(JsonTransformer jsonTransformer) {
        this.jsonTransformer = jsonTransformer;
    }

    AuthTaskResult handleErrorResponse(ApiResponse response) {
        if (response.isSuccess()) {
            throw new IllegalArgumentException("Responses passed to this method should not be successful");
        }

        ApiRequestException exception = response.getFailure();
        if (exception.reason() == BAD_REQUEST && exception.errorKey().equals(AGE_RESTRICTED)) {
            return handleAgeRestrictionError(response);
        }
        return handleApiRequestException(exception);
    }

    public static AuthTaskResult handleApiRequestException(ApiRequestException exception) {
        switch (exception.reason()) {
            case BAD_REQUEST:
                return handleBadRequest(exception);
            case AUTH_ERROR:
                return AuthTaskResult.unauthorized(exception);
            case VALIDATION_ERROR:
                return validationError(exception.errorKey(), exception);
            case NETWORK_ERROR:
                return AuthTaskResult.networkError((Exception) exception.getCause());
            case SERVER_ERROR:
                return AuthTaskResult.serverError(exception);
            case RATE_LIMITED:
                return handleRateLimitError(exception);
            case PRECONDITION_REQUIRED:
                return AuthTaskResult.spam(exception);
            case NOT_ALLOWED:
                return AuthTaskResult.denied(exception);
            default:
                return AuthTaskResult.failure(exception);
        }
    }

    private static AuthTaskResult handleBadRequest(ApiRequestException exception) {
        switch (exception.errorKey()) {
            case INCORRECT_CREDENTIALS:
                return AuthTaskResult.incorrectCredentials(exception);
            case EMAIL_TAKEN:
                return AuthTaskResult.emailTaken(exception);
            case SPAMMING:
                return AuthTaskResult.spam(exception);
            default:
                return AuthTaskResult.failure(exception);
        }
    }

    private AuthTaskResult handleAgeRestrictionError(ApiResponse response) {
        try {
            return AgeRestrictionAuthResult.create(getMinimumSignupAge(response.getResponseBody()));
        } catch (Exception e) {
            return handleApiRequestException(response.getFailure());
        }
    }

    private static AuthTaskResult handleRateLimitError(ApiRequestException exception) {
        switch (exception.errorKey()) {
            case INVALID_EMAIL:
                return AuthTaskResult.emailInvalid(exception);
            case DOMAIN_BLACKLISTED:
                return AuthTaskResult.denied(exception);
            default:
                return AuthTaskResult.failure(exception);
        }
    }

    private String getMinimumSignupAge(String responseBody) throws IOException, ApiMapperException {
        AgeRestrictionError ageRestrictionError = jsonTransformer.fromJson(responseBody, TypeToken.of(AgeRestrictionError.class));
        return ageRestrictionError.minimumAge();
    }
}
