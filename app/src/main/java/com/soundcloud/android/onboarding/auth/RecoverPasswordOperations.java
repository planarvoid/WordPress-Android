package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.onboarding.auth.request.ResetPasswordBody;

import javax.inject.Inject;

public class RecoverPasswordOperations {

    private final ApiClient apiClient;

    @Inject
    RecoverPasswordOperations(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public boolean recoverPassword(String email) {
        ApiResponse apiResponse = apiClient.fetchResponse(ApiRequest.post(ApiEndpoints.RESET_PASSWORD.path())
                                                                    .forPrivateApi()
                                                                    .withContent(ResetPasswordBody.create(email))
                                                                    .build());

        return apiResponse.isSuccess();
    }
}
