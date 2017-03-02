package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.creators.record.jni.VorbisConstants.getString;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.onboarding.auth.request.SignUpBody;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.profile.BirthdayInfo;

import android.content.Context;
import android.os.Bundle;

import javax.inject.Inject;
import java.io.IOException;

public class SignUpOperations {

    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_BIRTHDAY = "birthday";
    public static final String KEY_GENDER = "gender";

    private final ApiClient apiClient;
    private final OAuth oAuth;
    private final SoundCloudApplication applicationContext;
    private final ConfigurationOperations configurationOperations;

    @Inject
    public SignUpOperations(Context context,
                            ApiClient apiClient,
                            OAuth oAuth,
                            ConfigurationOperations configurationOperations) {
        applicationContext = (SoundCloudApplication) context.getApplicationContext();
        this.apiClient = apiClient;
        this.oAuth = oAuth;
        this.configurationOperations = configurationOperations;
    }

    public AuthTaskResult signUp(Bundle bundle) {
        final AuthTaskResult result = doSignup(bundle);

        if (result.wasSuccess()) {
            Token token = result.getAuthResponse().token;
            if (token == null || !applicationContext.addUserAccountAndEnableSync(result.getAuthResponse().me.getUser(), token, SignupVia.API)) {
                return AuthTaskResult.failure(applicationContext.getString(R.string.authentication_signup_error_message));
            }
            configurationOperations.saveConfiguration(result.getAuthResponse().me.getConfiguration());
        }
        return result;
    }

    private AuthTaskResult doSignup(Bundle bundle) {
        try {
            AuthResponse authResponse = performRequest(bundle);
            return AuthTaskResult.success(authResponse, SignupVia.API);
        } catch (TokenRetrievalException e) {
            return AuthTaskResult.failure(getString(R.string.signup_scope_revoked));
        } catch (ApiMapperException e) {
            return AuthTaskResult.failure(getString(R.string.authentication_signup_error_message));
        } catch (ApiRequestException e) {
            return getError(e);
        } catch (IOException e) {
            return AuthTaskResult.failure(e);
        }
    }

    private AuthResponse performRequest(Bundle bundle) throws ApiRequestException, IOException, ApiMapperException {
        final ApiRequest request = ApiRequest.post(ApiEndpoints.SIGN_UP.path())
                                             .forPrivateApi()
                                             .withContent(getSignUpBody(bundle))
                                             .build();

        return apiClient.fetchMappedResponse(request, AuthResponse.class);
    }

    private SignUpBody getSignUpBody(Bundle bundle) {
        final BirthdayInfo birthday = (BirthdayInfo) bundle.getSerializable(KEY_BIRTHDAY);
        return SignUpBody.create(oAuth.getClientId(),
                                 oAuth.getClientSecret(),
                                 bundle.getString(KEY_USERNAME),
                                 bundle.getString(KEY_PASSWORD),
                                 bundle.getString(KEY_GENDER),
                                 birthday.getYear(),
                                 birthday.getMonth());
    }

    private AuthTaskResult getError(ApiRequestException exception) {
        switch (exception.reason()) {
            case BAD_REQUEST:
                return AuthTaskResult.failure(exception);
            case NOT_ALLOWED:
                return AuthTaskResult.denied(exception);
            case SERVER_ERROR:
                return AuthTaskResult.failure(getString(R.string.error_server_problems_message), exception);
            case RATE_LIMITED:
                return getRateLimitError(exception);
            case PRECONDITION_REQUIRED:
                return AuthTaskResult.spam(exception);
            default:
                return AuthTaskResult.failure(getString(R.string.authentication_signup_error_message), exception);
        }
    }

    private AuthTaskResult getRateLimitError(ApiRequestException exception) {
        switch (exception.errorKey()) {
            case "invalid_email":
                return AuthTaskResult.emailInvalid(exception);
            case "domain_blacklisted":
                return AuthTaskResult.denied(exception);
            default:
                return AuthTaskResult.failure(exception);
        }
    }
}
