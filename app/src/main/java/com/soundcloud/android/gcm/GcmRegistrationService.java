package com.soundcloud.android.gcm;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;

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
    @Inject FeatureFlags featureFlags;
    @Inject AccountOperations accountOperations;
    @Inject ApplicationProperties applicationProperties;

    public static void startGcmService(Context context){
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
                           FeatureFlags featureFlags,
                           AccountOperations accountOperations,
                           ApplicationProperties applicationProperties) {
        super(TAG);
        this.gcmStorage = gcmStorage;
        this.apiClient = apiClient;
        this.instanceId = instanceId;
        this.appboyWrapperProvider = appboyWrapperProvider;
        this.featureFlags = featureFlags;
        this.accountOperations = accountOperations;
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (accountOperations.isUserLoggedIn() && gcmStorage.shouldRegister()) {
            doTokenRegistration();
        }
    }

    private void doTokenRegistration() {
        final String token = getToken();
        if (token != null) {
            Log.d(TAG, "Push Registration Token: " + token);
            appboyWrapperProvider.get().handleRegistration(token);

            if (!applicationProperties.registerForGcm() || registerTokenWithApi(token).isSuccess()) {
                gcmStorage.markAsRegistered(token);
            }
        }
    }

    private String getToken() {
        final String storedToken = gcmStorage.getToken();
        return storedToken == null ? instanceId.getToken() : storedToken;
    }

    private ApiResponse registerTokenWithApi(String token) {
        final ApiRequest request = ApiRequest.post(ApiEndpoints.GCM_REGISTER.path())
                .forPrivateApi()
                .withContent(Collections.singletonMap("token", token))
                .build();

        return apiClient.fetchResponse(request);
    }
}
