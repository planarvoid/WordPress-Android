package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.Params;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.storage.LegacyUserStorage;

import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArrayMap;

import java.io.IOException;
import java.util.Map;

public class SignupTask extends AuthTask {
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_BIRTHDAY = "birthday";
    public static final String KEY_GENDER = "gender";

    private TokenInformationGenerator tokenUtils;
    private ApiClient apiClient;

    public SignupTask(SoundCloudApplication soundCloudApplication, LegacyUserStorage userStorage, TokenInformationGenerator tokenUtils, ApiClient apiClient) {
        super(soundCloudApplication, userStorage);
        this.tokenUtils = tokenUtils;
        this.apiClient = apiClient;
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        final SoundCloudApplication app = getSoundCloudApplication();
        final AuthTaskResult result = doSignup(params[0]);

        if (result.wasSuccess()) {
            try {
                final Token token = tokenUtils.getToken(params[0]);
                if (token == null || !app.addUserAccountAndEnableSync(result.getUser(), token, SignupVia.API)) {
                    return AuthTaskResult.failure(app.getString(R.string.authentication_signup_error_message));
                }
            } catch (ApiRequestException e) {
                return AuthTaskResult.signUpFailedToLogin(e);
            }
        }
        return result;
    }

    @VisibleForTesting
    AuthTaskResult doSignup(Bundle parameters) {
        try {
            final ApiRequest request = ApiRequest.post(ApiEndpoints.LEGACY_USERS.path())
                    .forPublicApi()
                    .withFormMap(signupParameters(parameters))
                    .withToken(tokenUtils.verifyScopes(Token.SCOPE_SIGNUP))
                    .build();

            PublicApiUser user = apiClient.fetchMappedResponse(request, PublicApiUser.class);
            return AuthTaskResult.success(user, SignupVia.API, false);
        } catch (TokenRetrievalException e) {
            return AuthTaskResult.failure(getString(R.string.signup_scope_revoked));
        } catch (ApiMapperException e) {
            return AuthTaskResult.failure(getString(R.string.authentication_signup_error_message));
        } catch (ApiRequestException e) {
            return handleError(e);
        } catch (IOException e) {
            return AuthTaskResult.failure(e);
        }
    }

    private AuthTaskResult handleError(ApiRequestException exception) {
        switch (exception.reason()) {
            case VALIDATION_ERROR:
                return handleUnprocessableEntity(exception.errorCode(), exception);
            case NOT_ALLOWED:
                return AuthTaskResult.denied(exception);
            case SERVER_ERROR:
                return AuthTaskResult.failure(getString(R.string.error_server_problems_message), exception);
            default:
                return AuthTaskResult.failure(getString(R.string.authentication_signup_error_message), exception);
        }
    }

    private AuthTaskResult handleUnprocessableEntity(int errorCode, ApiRequestException exception) {
        switch (errorCode) {
            case SignupResponseBody.ERROR_EMAIL_TAKEN:
                return AuthTaskResult.emailTaken(exception);
            case SignupResponseBody.ERROR_DOMAIN_BLACKLISTED:
                return AuthTaskResult.denied(exception);
            case SignupResponseBody.ERROR_CAPTCHA_REQUIRED:
                return AuthTaskResult.spam(exception);
            case SignupResponseBody.ERROR_EMAIL_INVALID:
                return AuthTaskResult.emailInvalid(exception);
            case SignupResponseBody.ERROR_OTHER:
                return AuthTaskResult.failure(getString(R.string.authentication_email_other_error_message), exception);
            default:
                return AuthTaskResult.failure(getString(R.string.authentication_signup_error_message), exception);
        }
    }

    private Map<String, String> signupParameters(Bundle parameters) {
        final BirthdayInfo birthday = (BirthdayInfo) parameters.getSerializable(KEY_BIRTHDAY);
        final ArrayMap<String, String> params = new ArrayMap<>(7);

        params.put(Params.User.EMAIL, parameters.getString(KEY_USERNAME));
        params.put(Params.User.PASSWORD, parameters.getString(KEY_PASSWORD));
        params.put(Params.User.PASSWORD_CONFIRMATION, parameters.getString(KEY_PASSWORD));
        params.put(Params.User.TERMS_OF_USE, "1");

        String gender = parameters.getString(KEY_GENDER);
        if (gender != null) {
            params.put(Params.User.GENDER, gender);
        }

        params.put(Params.User.DATE_OF_BIRTH_MONTH, String.valueOf(birthday.getMonth()));
        params.put(Params.User.DATE_OF_BIRTH_YEAR, String.valueOf(birthday.getYear()));

        return params;
    }
}
