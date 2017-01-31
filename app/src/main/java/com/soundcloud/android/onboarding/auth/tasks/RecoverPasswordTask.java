package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_FOUND;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.onboarding.auth.RecoverPasswordOperations;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

public class RecoverPasswordTask extends AsyncTask<String, Void, Boolean> {
    private final TokenInformationGenerator tokenInformationGenerator;
    private final OAuth oAuth;
    private final ApiClient apiClient;
    private final Resources resources;
    private final FeatureFlags featureFlags;
    private final RecoverPasswordOperations recoverPasswordOperations;
    protected String reason;

    protected RecoverPasswordTask(TokenInformationGenerator tokenInformationGenerator,
                                  OAuth oAuth,
                                  ApiClient apiClient,
                                  Resources resources,
                                  FeatureFlags featureFlags,
                                  RecoverPasswordOperations recoverPasswordOperations) {
        this.tokenInformationGenerator = tokenInformationGenerator;
        this.oAuth = oAuth;
        this.apiClient = apiClient;
        this.resources = resources;
        this.featureFlags = featureFlags;
        this.recoverPasswordOperations = recoverPasswordOperations;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        if (featureFlags.isEnabled(Flag.AUTH_API_MOBILE)) {
            ApiResponse apiResponse = recoverPasswordOperations.recoverPassword(params[0]);

            if (apiResponse.isNotSuccess() && apiResponse.getFailure().reason() == ApiRequestException.Reason.VALIDATION_ERROR) {
                reason = resources.getString(R.string.authentication_recover_password_unknown_email_address);
            }

            return apiResponse.isSuccess();
        } else {
            return legacyRecoverPassword(params);
        }
    }

    @NonNull
    @Deprecated // this uses public API
    private Boolean legacyRecoverPassword(String... params) {
        final String email = params[0];

        ApiRequestException failure;
        try {
            Token signup = tokenInformationGenerator.requestToken(oAuth.getTokenRequestParamsFromClientCredentials());
            ApiResponse apiResponse = apiClient.fetchResponse(ApiRequest.post(ApiEndpoints.LEGACY_RESET_PASSWORD.path())
                                                                        .forPublicApi()
                                                                        .addQueryParam("email", email)
                                                                        .withToken(featureFlags, signup)
                                                                        .build());
            failure = apiResponse.getFailure();
        } catch (ApiRequestException e) {
            e.printStackTrace();
            failure = e;
        }

        if (failure != null) {
            if (NOT_FOUND.equals(failure.reason())) {
                reason = resources.getString(R.string.authentication_recover_password_unknown_email_address);
            }
            return false;
        } else {
            return true;
        }
    }
}
