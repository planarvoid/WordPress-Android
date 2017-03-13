package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.creators.record.jni.VorbisConstants.getString;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.onboarding.auth.request.SignUpBody;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.java.reflect.TypeToken;

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
    private final JsonTransformer jsonTransformer;
    private final AuthResultMapper authResultMapper;
    private final OAuth oAuth;
    private final SoundCloudApplication applicationContext;
    private final ConfigurationOperations configurationOperations;

    @Inject
    public SignUpOperations(Context context,
                            ApiClient apiClient,
                            JsonTransformer jsonTransformer,
                            AuthResultMapper authResultMapper,
                            OAuth oAuth,
                            ConfigurationOperations configurationOperations) {
        applicationContext = (SoundCloudApplication) context.getApplicationContext();
        this.apiClient = apiClient;
        this.jsonTransformer = jsonTransformer;
        this.authResultMapper = authResultMapper;
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
        } catch (IOException e) {
            return AuthTaskResult.failure(e);
        } catch (SignupError signupError) {
            return signupError.result;
        }
    }

    private AuthResponse performRequest(Bundle bundle) throws IOException, ApiMapperException, SignupError {
        final ApiRequest request = ApiRequest.post(ApiEndpoints.SIGN_UP.path())
                                             .forPrivateApi()
                                             .withContent(getSignUpBody(bundle))
                                             .build();

        ApiResponse apiResponse = apiClient.fetchResponse(request);
        if (apiResponse.isSuccess()) {
            return jsonTransformer.fromJson(apiResponse.getResponseBody(), TypeToken.of(AuthResponse.class));
        } else {
            throw new SignupError(authResultMapper.handleErrorResponse(apiResponse));
        }
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


    private class SignupError extends Throwable {
        public final AuthTaskResult result;

        SignupError(AuthTaskResult result) {
            this.result = result;
        }
    }
}
