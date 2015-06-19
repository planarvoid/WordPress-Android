package com.soundcloud.android.onboarding.auth.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.DeviceManagement;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.onboarding.exceptions.AddAccountException;
import com.soundcloud.android.onboarding.exceptions.SignInException;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.tasks.FetchUserTask;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.os.Bundle;

import static android.util.Log.INFO;
import static com.soundcloud.android.utils.ErrorUtils.log;
import static com.soundcloud.android.utils.Log.ONBOARDING_TAG;

import javax.inject.Inject;

public class LoginTask extends AuthTask {

    @Inject TokenInformationGenerator tokenUtils;

    @VisibleForTesting
    static String CONFLICTING_DEVICE_KEY = "conflictingDeviceKey";

    private FetchUserTask fetchUserTask;
    private final ConfigurationOperations configurationOperations;
    private final EventBus eventBus;
    private final AccountOperations accountOperations;

    protected LoginTask(@NotNull SoundCloudApplication application, TokenInformationGenerator tokenUtils,
                        FetchUserTask fetchUserTask, LegacyUserStorage userStorage, ConfigurationOperations configurationOperations,
                        EventBus eventBus, AccountOperations accountOperations) {
        super(application, userStorage);
        this.tokenUtils = tokenUtils;
        this.fetchUserTask = fetchUserTask;
        this.configurationOperations = configurationOperations;
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
    }

    public LoginTask(@NotNull SoundCloudApplication application, ConfigurationOperations configurationOperations,
                     EventBus eventBus, AccountOperations accountOperations, TokenInformationGenerator tokenUtils){
        this(application, tokenUtils,
                new FetchUserTask(new PublicApi(application)), new LegacyUserStorage(), configurationOperations, eventBus, accountOperations);
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        return login(params[0]);
    }

    protected AuthTaskResult login(Bundle data) {
        Context app = getSoundCloudApplication();

        try {
            Token token = tokenUtils.getToken(data);

            String conflictingDeviceId = data.getString(CONFLICTING_DEVICE_KEY);

            if (ScTextUtils.isBlank(conflictingDeviceId)) {
                DeviceManagement deviceManagement = configurationOperations.registerDevice(token);
                if (deviceManagement.isNotAuthorized()) {
                    data.putString(CONFLICTING_DEVICE_KEY, deviceManagement.getConflictingDeviceId());
                    return AuthTaskResult.deviceConflict(data);
                }

            } else {
                DeviceManagement deviceManagement = configurationOperations.forceRegisterDevice(token, conflictingDeviceId);
                if (deviceManagement.isNotAuthorized()) {
                    return AuthTaskResult.failure(app.getString(R.string.error_server_problems_message));
                }
            }

            accountOperations.updateToken(token);

            final PublicApiUser user = fetchUserTask.resolve(Request.to(Endpoints.MY_DETAILS));
            if (user == null) {
                return AuthTaskResult.failure(app.getString(R.string.authentication_error_no_connection_message));
            }

            SignupVia signupVia = token.getSignup() != null ? SignupVia.fromString(token.getSignup()) : SignupVia.NONE;
            if (!addAccount(user, token, signupVia)) {
                // might mean the account already existed or an unknown failure adding account.
                // this should never happen, just show a generic error message
                ErrorUtils.handleSilentException(new AddAccountException());
                return AuthTaskResult.failure(app.getString(R.string.authentication_login_error_message));
            }

            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.authComplete());

            return AuthTaskResult.success(user, signupVia, tokenUtils.isFromFacebook(data));


        } catch (Exception e) {
            log(INFO, ONBOARDING_TAG, "error retrieving SC API token: " + e.getMessage());
            return AuthTaskResult.failure(new TokenRetrievalException(e));
        }
    }
}
