package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.onboarding.auth.AuthResultMapper.DOMAIN_BLACKLISTED;
import static com.soundcloud.android.onboarding.auth.AuthResultMapper.EMAIL_TAKEN;
import static com.soundcloud.android.onboarding.auth.AuthResultMapper.INCORRECT_CREDENTIALS;
import static com.soundcloud.android.onboarding.auth.AuthResultMapper.INVALID_EMAIL;
import static com.soundcloud.android.onboarding.auth.AuthResultMapper.SPAMMING;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.onboarding.auth.tasks.AgeRestrictionAuthResult;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class AuthResultMapperTest extends AndroidUnitTest {

    private AuthResultMapper mapper;

    @Before
    public void setUp() throws Exception {
        mapper = new AuthResultMapper(new JacksonJsonTransformer());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenHandlingSuccess() throws Exception {
        ApiResponse apiResponse = new ApiResponse(null, 200, "");
        mapper.handleErrorResponse(apiResponse);
    }

    @Test
    public void ageRestrictionError() throws Exception {
        String minimumAge = "16";
        String response = "{\"error_key\": \"age_restricted\", \"minimum_age\": " + minimumAge + "}";
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(new ApiResponse(null, 400, response));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasAgeRestricted()).isTrue();
        assertThat(((AgeRestrictionAuthResult) authTaskResult).getMinimumAge()).isEqualTo(minimumAge);
    }

    @Test
    public void wasValidationError() throws Exception {
        String errorKey = "errorKey";
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(422, errorKey));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasAgeRestricted()).isFalse();
        assertThat(authTaskResult.wasValidationError()).isTrue();
    }

    @Test
    public void incorrectCredentials() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(400, INCORRECT_CREDENTIALS));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasUnauthorized()).isTrue();
    }

    @Test
    public void emailTaken() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(400, EMAIL_TAKEN));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasEmailTaken()).isTrue();
    }

    @Test
    public void spam() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(400, SPAMMING));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasSpam()).isTrue();
    }

    @Test
    public void badRequest() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(400, "error"));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasFailure()).isTrue();
    }

    @Test
    public void authError() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(401, "error"));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasUnauthorized()).isTrue();
    }

    @Test
    public void networkError() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(new ApiResponse(ApiRequestException.networkError(null, new IOException())));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasNetworkError()).isTrue();
    }

    @Test
    public void serverError() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(500, "whoops"));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasServerError()).isTrue();
    }

    @Test
    public void invalidEmail() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(429, INVALID_EMAIL));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasEmailInvalid()).isTrue();
    }

    @Test
    public void domainBlacklisted() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(429, DOMAIN_BLACKLISTED));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasDenied()).isTrue();
    }

    @Test
    public void rateLimitedFailure() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(429, "some error"));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasFailure()).isTrue();
    }

    @Test
    public void preconditionRequired() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(428, "some error"));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasSpam()).isTrue();
    }

    @Test
    public void notAllowed() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(403, "some error"));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasDenied()).isTrue();
    }

    @Test
    public void otherFailure() throws Exception {
        AuthTaskResult authTaskResult = mapper.handleErrorResponse(responseWithError(488, "some error"));

        assertThat(authTaskResult.wasSuccess()).isFalse();
        assertThat(authTaskResult.wasFailure()).isTrue();
    }

    private ApiResponse responseWithError(int statusCode, String errorKey) {
        return new ApiResponse(null, statusCode, getResponse(errorKey));
    }

    private String getResponse(String errorKey) {
        return String.format("{\n\"error_key\": \"%s\"\n}", errorKey);
    }
}
