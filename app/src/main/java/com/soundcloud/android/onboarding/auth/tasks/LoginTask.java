package com.soundcloud.android.onboarding.auth.tasks;

import static android.util.Log.INFO;
import static com.soundcloud.android.onboarding.OnboardActivity.ONBOARDING_TAG;
import static com.soundcloud.android.utils.ErrorUtils.log;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.DeviceManagement;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.onboarding.exceptions.AddAccountException;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

public class LoginTask extends AuthTask {

    @VisibleForTesting
    static String IS_CONFLICTING_DEVICE = "isConflictingDevice";

    private final ConfigurationOperations configurationOperations;
    private final EventBus eventBus;
    protected final AccountOperations accountOperations;
    private final ApiClient apiClient;
    protected final TokenInformationGenerator tokenUtils;

    public LoginTask(@NotNull SoundCloudApplication application, TokenInformationGenerator tokenUtils,
                     StoreUsersCommand storeUsersCommand, ConfigurationOperations configurationOperations,
                     EventBus eventBus, AccountOperations accountOperations, ApiClient apiClient) {
        super(application, storeUsersCommand);
        this.tokenUtils = tokenUtils;
        this.configurationOperations = configurationOperations;
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.apiClient = apiClient;
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        return login(params[0]);
    }

    protected AuthTaskResult login(Bundle data) {
        try {
            Token token = tokenUtils.getToken(data);
            boolean isConflictingDevice = data.getBoolean(IS_CONFLICTING_DEVICE);

            if (isConflictingDevice) {
                DeviceManagement deviceManagement = configurationOperations.forceRegisterDevice(token);
                if (deviceManagement.isUnauthorized()) {
                    // Still unauthorized after force register. Fail with generic error to avoid looping.
                    return AuthTaskResult.failure(getString(R.string.error_server_problems_message));
                }
            } else {
                DeviceManagement deviceManagement = configurationOperations.registerDevice(token);
                if (deviceManagement.isRecoverableBlock()) {
                    // 3 active device limit. Can be force registered by replacing conflicting device.
                    data.putBoolean(IS_CONFLICTING_DEVICE, true);
                    return AuthTaskResult.deviceConflict(data);
                } else if (deviceManagement.isUnrecoverableBlock()) {
                    // 10 registered device limit. Cannot proceed until another device de-registers.
                    return AuthTaskResult.deviceBlock();
                }
            }

            accountOperations.updateToken(token);

            final Me me = apiClient.fetchMappedResponse(buildRequest(), new TypeToken<Me>() {
            });
            if (me == null) {
                return AuthTaskResult.failure(getString(R.string.authentication_error_no_connection_message));
            }

            SignupVia signupVia = token.getSignup() != null ? SignupVia.fromString(token.getSignup()) : SignupVia.NONE;
            if (!addAccount(me.getUser(), token, signupVia)) {
                ErrorUtils.handleSilentException(new AddAccountException());
                return AuthTaskResult.failure(getString(R.string.authentication_login_error_message));
            }

            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.authComplete());
            return AuthTaskResult.success(me.getUser(), signupVia, tokenUtils.isFromFacebook(data));
        } catch (ApiRequestException e) {
            log(INFO, ONBOARDING_TAG, "error logging in: " + e.getMessage());
            return AuthTaskResult.failure(e);
        } catch (Exception e) {
            log(INFO, ONBOARDING_TAG, "error retrieving SC API token: " + e.getMessage());
            return AuthTaskResult.failure(new TokenRetrievalException(e));
        }
    }

    protected ApiRequest buildRequest() {
        return ApiRequest.get(ApiEndpoints.ME.path())
                         .forPrivateApi()
                         .build();
    }
}
