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
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.onboarding.auth.SignUpOperations;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.SyncInitiatorBridge;

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
    private final FeatureFlags featureFlags;
    private final SignUpOperations signUpOperations;

    public SignupTask(SoundCloudApplication soundCloudApplication,
                      StoreUsersCommand storeUsersCommand,
                      TokenInformationGenerator tokenUtils,
                      ApiClient apiClient,
                      SyncInitiatorBridge syncInitiatorBridge,
                      FeatureFlags featureFlags,
                      SignUpOperations signUpOperations) {
        super(soundCloudApplication, storeUsersCommand, syncInitiatorBridge);
        this.tokenUtils = tokenUtils;
        this.apiClient = apiClient;
        this.featureFlags = featureFlags;
        this.signUpOperations = signUpOperations;
    }

    @Override
    protected LegacyAuthTaskResult doInBackground(Bundle... params) {
        if (featureFlags.isEnabled(Flag.AUTH_API_MOBILE)) {
            AuthTaskResult result = signUpOperations.signUp(params[0]);
            return LegacyAuthTaskResult.fromAuthTaskResult(result);
        } else {
            return legacySignup(params[0]);
        }
    }

    @Deprecated // this uses public API
    private LegacyAuthTaskResult legacySignup(Bundle parameters) {
        final SoundCloudApplication app = getSoundCloudApplication();
        final LegacyAuthTaskResult result = doSignup(parameters);

        if (result.wasSuccess()) {
            try {
                final Token token = tokenUtils.getToken(parameters);
                if (token == null || !app.addUserAccountAndEnableSync(result.getUser(), token, SignupVia.API)) {
                    return LegacyAuthTaskResult.failure(app.getString(R.string.authentication_signup_error_message));
                }
            } catch (ApiRequestException e) {
                return LegacyAuthTaskResult.signUpFailedToLogin(e);
            }
        }
        return result;
    }

    @VisibleForTesting
    LegacyAuthTaskResult doSignup(Bundle parameters) {
        try {
            final ApiRequest request = ApiRequest.post(ApiEndpoints.LEGACY_USERS.path())
                                                 .forPublicApi()
                                                 .withFormMap(signupParameters(parameters))
                                                 .withToken(featureFlags, tokenUtils.verifyScopes(Token.SCOPE_SIGNUP))
                                                 .build();

            ApiUser user = apiClient.fetchMappedResponse(request, PublicApiUser.class).toApiMobileUser();
            return LegacyAuthTaskResult.success(user, SignupVia.API);
        } catch (TokenRetrievalException e) {
            return LegacyAuthTaskResult.failure(getString(R.string.signup_scope_revoked));
        } catch (ApiMapperException e) {
            return LegacyAuthTaskResult.failure(getString(R.string.authentication_signup_error_message));
        } catch (ApiRequestException e) {
            return handleError(e);
        } catch (IOException e) {
            return LegacyAuthTaskResult.failure(e);
        }
    }

    private LegacyAuthTaskResult handleError(ApiRequestException exception) {
        switch (exception.reason()) {
            case VALIDATION_ERROR:
                return handleUnprocessableEntity(exception.errorCode(), exception);
            case NOT_ALLOWED:
                return LegacyAuthTaskResult.denied(exception);
            case SERVER_ERROR:
                return LegacyAuthTaskResult.failure(getString(R.string.error_server_problems_message), exception);
            default:
                return LegacyAuthTaskResult.failure(getString(R.string.authentication_signup_error_message), exception);
        }
    }

    private LegacyAuthTaskResult handleUnprocessableEntity(int errorCode, ApiRequestException exception) {
        switch (errorCode) {
            case SignupResponseBody.ERROR_EMAIL_TAKEN:
                return LegacyAuthTaskResult.emailTaken(exception);
            case SignupResponseBody.ERROR_DOMAIN_BLACKLISTED:
                return LegacyAuthTaskResult.denied(exception);
            case SignupResponseBody.ERROR_CAPTCHA_REQUIRED:
                return LegacyAuthTaskResult.spam(exception);
            case SignupResponseBody.ERROR_EMAIL_INVALID:
                return LegacyAuthTaskResult.emailInvalid(exception);
            case SignupResponseBody.ERROR_OTHER:
                return LegacyAuthTaskResult.failure(getString(R.string.authentication_email_other_error_message), exception);
            default:
                return LegacyAuthTaskResult.failure(getString(R.string.authentication_signup_error_message), exception);
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
