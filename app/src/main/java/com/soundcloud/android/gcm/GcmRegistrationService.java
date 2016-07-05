package com.soundcloud.android.gcm;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collections;

public class GcmRegistrationService extends IntentService {

    private static final String TAG = "GcmRegistrationService";

    @Inject GcmStorage gcmStorage;
    @Inject ApiClient apiClient;
    @Inject InstanceIdWrapper instanceId;
    @Inject Provider<AppboyWrapper> appboyWrapperProvider;
    @Inject AccountOperations accountOperations;
    @Inject FeatureFlags featureFlags;

    public static void startGcmService(Context context) {
        context.startService(new Intent(context, GcmRegistrationService.class));
    }

    public GcmRegistrationService() {
        super(TAG);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    GcmRegistrationService(GcmStorage gcmStorage,
                           ApiClient apiClient,
                           InstanceIdWrapper instanceId,
                           Provider<AppboyWrapper> appboyWrapperProvider,
                           AccountOperations accountOperations,
                           FeatureFlags featureFlags) {
        super(TAG);
        this.gcmStorage = gcmStorage;
        this.apiClient = apiClient;
        this.instanceId = instanceId;
        this.appboyWrapperProvider = appboyWrapperProvider;
        this.accountOperations = accountOperations;
        this.featureFlags = featureFlags;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (accountOperations.isUserLoggedIn() && gcmStorage.shouldRegister()) {
            doTokenRefresh();
        }
    }

    private void doTokenRefresh() {
        String token = instanceId.getToken();
        if (token != null) {
            Log.d(TAG, "Push Registration Token: " + token);
            appboyWrapperProvider.get().handleRegistration(token);

            if (featureFlags.isDisabled(Flag.ARCHER_PUSH) || registerTokenWithApi(token).isSuccess()) {
                gcmStorage.markAsRegistered(token);
            }
        } else {
            gcmStorage.clearHasRegistered();
        }
    }

    private ApiResponse registerTokenWithApi(String token) {
        final ApiRequest request = ApiRequest.post(ApiEndpoints.GCM_REGISTER.path())
                                             .forPrivateApi()
                                             .withContent(Collections.singletonMap("token", token))
                                             .build();

        return apiClient.fetchResponse(request);
    }
}
