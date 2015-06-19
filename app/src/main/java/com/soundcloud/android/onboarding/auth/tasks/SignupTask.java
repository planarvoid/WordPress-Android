package com.soundcloud.android.onboarding.auth.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.Bundle;

import javax.inject.Inject;
import java.io.IOException;

public class SignupTask extends AuthTask {

    private static final String TAG = SignupTask.class.getSimpleName();
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_BIRTHDAY = "birthday";
    public static final String KEY_GENDER = "gender";

    @Inject TokenInformationGenerator tokenInformationGenerator;
    @Inject PublicCloudAPI publicApi;

    protected SignupTask(SoundCloudApplication application,
                         LegacyUserStorage userStorage) {
        super(application, userStorage);
        SoundCloudApplication.getObjectGraph().inject(this);

    }

    public SignupTask(SoundCloudApplication soundCloudApplication){
        this(soundCloudApplication, new LegacyUserStorage());
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        final SoundCloudApplication app = getSoundCloudApplication();

        AuthTaskResult result = doSignup(app, params[0]);
        if (result.wasSuccess()){
            // do token exchange
            final Token token;
            try {
                token = tokenInformationGenerator.getToken(params[0]);
                if (token == null || !app.addUserAccountAndEnableSync(result.getUser(), token, SignupVia.API)) {
                    return AuthTaskResult.failure(app.getString(R.string.authentication_signup_error_message));
                }
            } catch (ApiRequestException e) {
                return AuthTaskResult.signUpFailedToLogin();
            }
        }
        return result;
    }

    @VisibleForTesting
    AuthTaskResult doSignup(SoundCloudApplication application, Bundle parameters){
        try {
            // explicitly request signup scope
            final Token signupToken = publicApi.clientCredentials(Token.SCOPE_SIGNUP);
            Log.d(TAG, signupToken.toString());

            final HttpResponse response = postSignupRequest(parameters, signupToken);

            int statusCode = response.getStatusLine().getStatusCode();
            switch (statusCode) {
                case HttpStatus.SC_CREATED: // success case
                    return handleSuccess(response);
                case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                    return handleUnprocessableEntity(application, response);
                case HttpStatus.SC_FORBIDDEN:
                    return AuthTaskResult.denied();
                default:
                    return handleServerErrorsAndUnexpectedStatus(application, statusCode);
            }
        } catch (CloudAPI.InvalidTokenException e) {
            return AuthTaskResult.failure(application.getString(R.string.signup_scope_revoked));
        } catch (JsonProcessingException e) {
            // Some types of responses still do not adhere to the new response body structure, like invalid email
            // responses from mothership. Until then, this generic error will be shown
            return AuthTaskResult.failure(application.getString(R.string.authentication_signup_error_message));
        } catch (IOException e) {
            return AuthTaskResult.failure(e);
        }
    }

    private HttpResponse postSignupRequest(Bundle parameters, Token signupToken) throws IOException {
        BirthdayInfo birthday = (BirthdayInfo) parameters.getSerializable(KEY_BIRTHDAY);
        return publicApi.post(Request.to(Endpoints.USERS).with(
                Params.User.EMAIL, parameters.getString(KEY_USERNAME),
                Params.User.PASSWORD, parameters.getString(KEY_PASSWORD),
                Params.User.PASSWORD_CONFIRMATION, parameters.getString(KEY_PASSWORD),
                Params.User.TERMS_OF_USE, "1",
                Params.User.GENDER, parameters.getString(KEY_GENDER),
                Params.User.DATE_OF_BIRTH_MONTH, birthday.getMonth(),
                Params.User.DATE_OF_BIRTH_YEAR, birthday.getYear()
        ).usingToken(signupToken));
    }

    private AuthTaskResult handleSuccess(HttpResponse response) throws IOException {
        final PublicApiUser user = publicApi.getMapper().readValue(response.getEntity().getContent(), PublicApiUser.class);
        return AuthTaskResult.success(user, SignupVia.API, false);
    }

    private AuthTaskResult handleUnprocessableEntity(SoundCloudApplication application, HttpResponse response) throws IOException {
        final SignupResponseBody signupResponseBody = publicApi.getMapper().readValue(response.getEntity().getContent(), SignupResponseBody.class);
        switch (signupResponseBody.getError()) {
            case SignupResponseBody.ERROR_EMAIL_TAKEN:
                return AuthTaskResult.emailTaken();
            case SignupResponseBody.ERROR_DOMAIN_BLACKLISTED:
                return AuthTaskResult.denied();
            case SignupResponseBody.ERROR_CAPTCHA_REQUIRED:
                return AuthTaskResult.spam();
            case SignupResponseBody.ERROR_EMAIL_INVALID:
                return AuthTaskResult.emailInvalid();
            case SignupResponseBody.ERROR_OTHER:
                return AuthTaskResult.failure(application.getString(R.string.authentication_email_other_error_message));
            default:
                return AuthTaskResult.failure(application.getString(R.string.authentication_signup_error_message));
        }
    }

    private AuthTaskResult handleServerErrorsAndUnexpectedStatus(SoundCloudApplication application, int statusCode) {
        if (statusCode >= 500) {
            return AuthTaskResult.failure(application.getString(R.string.error_server_problems_message));
        } else {
            return AuthTaskResult.failure(application.getString(R.string.authentication_signup_error_message));
        }
    }
}
