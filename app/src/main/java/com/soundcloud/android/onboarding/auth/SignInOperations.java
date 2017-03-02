package com.soundcloud.android.onboarding.auth;

import static android.util.Log.INFO;
import static com.soundcloud.android.creators.record.jni.VorbisConstants.getString;
import static com.soundcloud.android.onboarding.OnboardActivity.ONBOARDING_TAG;
import static com.soundcloud.android.utils.ErrorUtils.log;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.DeviceManagement;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.onboarding.auth.request.SignInBody;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.onboarding.exceptions.AddAccountException;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.io.IOException;

public class SignInOperations {

    public static final String FACEBOOK_TOKEN_EXTRA = "facebook";
    public static final String GOOGLE_TOKEN_EXTRA = "google";
    @VisibleForTesting static final String USERNAME_EXTRA = "username";
    @VisibleForTesting static final String PASSWORD_EXTRA = "password";

    @VisibleForTesting
    static String IS_CONFLICTING_DEVICE = "isConflictingDevice";
    private final ConfigurationOperations configurationOperations;
    private final EventBus eventBus;
    private final AccountOperations accountOperations;
    private final Context context;
    private final ApiClient apiClient;
    private final OAuth oAuth;

    @Inject
    public SignInOperations(Context context,
                            ApiClient apiClient,
                            OAuth oAuth,
                            ConfigurationOperations configurationOperations,
                            EventBus eventBus,
                            AccountOperations accountOperations) {
        this.context = context;
        this.apiClient = apiClient;
        this.oAuth = oAuth;
        this.configurationOperations = configurationOperations;
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
    }

    public AuthTaskResult signIn(Bundle data) {
        try {
            final AuthResponse loginResponse = performSignIn(data);
            final Token token = loginResponse.token;

            Optional<AuthTaskResult> failedToRegisterDeviceResult = handleDeviceManagement(data, token);
            if (failedToRegisterDeviceResult.isPresent()) {
                return failedToRegisterDeviceResult.get();
            }

            accountOperations.updateToken(token);

            SignupVia signupVia = token.getSignup() != null ? SignupVia.fromString(token.getSignup()) : SignupVia.NONE;
            if (!addAccount(loginResponse, token, signupVia)) {
                ErrorUtils.handleSilentException(new AddAccountException());
                return AuthTaskResult.failure(getString(R.string.authentication_login_error_message));
            }

            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.authComplete());
            return AuthTaskResult.success(loginResponse, signupVia);
        } catch (ApiRequestException e) {
            log(INFO, ONBOARDING_TAG, "error logging in: " + e.getMessage());
            return AuthTaskResult.failure(e);
        } catch (Exception e) {
            log(INFO, ONBOARDING_TAG, "error retrieving SC API token: " + e.getMessage());
            return AuthTaskResult.failure(new TokenRetrievalException(e));
        }
    }

    private boolean addAccount(AuthResponse loginResponse, Token token, SignupVia signupVia) {
        return ((SoundCloudApplication) context.getApplicationContext()).addUserAccountAndEnableSync(loginResponse.me.getUser(), token, signupVia);
    }

    private Optional<AuthTaskResult> handleDeviceManagement(Bundle data, Token token) throws ApiRequestException, IOException, ApiMapperException {
        boolean isConflictingDevice = data.getBoolean(IS_CONFLICTING_DEVICE);

        if (isConflictingDevice) {
            DeviceManagement deviceManagement = configurationOperations.forceRegisterDevice(token);
            if (deviceManagement.isUnauthorized()) {
                // Still unauthorized after force register. Fail with generic error to avoid looping.
                return Optional.of(AuthTaskResult.failure(getString(R.string.error_server_problems_message)));
            }
        } else {
            DeviceManagement deviceManagement = configurationOperations.registerDevice(token);
            if (deviceManagement.isRecoverableBlock()) {
                // 3 active device limit. Can be force registered by replacing conflicting device.
                data.putBoolean(IS_CONFLICTING_DEVICE, true);
                return Optional.of(AuthTaskResult.deviceConflict(data));
            } else if (deviceManagement.isUnrecoverableBlock()) {
                // 10 registered device limit. Cannot proceed until another device de-registers.
                return Optional.of(AuthTaskResult.deviceBlock());
            }
        }
        return Optional.absent();
    }

    public static Bundle getFacebookTokenBundle(String token) {
        Bundle bundle = new Bundle();
        bundle.putString(FACEBOOK_TOKEN_EXTRA, token);
        return bundle;
    }

    public static Bundle getGoogleTokenBundle(String token) {
        Bundle bundle = new Bundle();
        bundle.putString(GOOGLE_TOKEN_EXTRA, token);
        return bundle;
    }

    private AuthResponse performSignIn(Bundle data) throws ApiRequestException {
        final ApiRequest request = ApiRequest.post(ApiEndpoints.SIGN_IN.path())
                                             .forPrivateApi()
                                             .withContent(getSignInBody(data))
                                             .build();
        try {
            return apiClient.fetchMappedResponse(request, AuthResponse.class);
        } catch (IOException | ApiMapperException e) {
            throw new TokenRetrievalException(e);
        }
    }

    private SignInBody getSignInBody(Bundle bundle) {
        if (isFromUserCredentials(bundle)) {
            return SignInBody.withUserCredentials(getUsername(bundle), getPassword(bundle), oAuth.getClientId(), oAuth.getClientSecret());
        } else if (isFromFacebook(bundle)) {
            return SignInBody.withFacebookToken(bundle.getString(FACEBOOK_TOKEN_EXTRA), oAuth.getClientId(), oAuth.getClientSecret());
        } else if (isFromGoogle(bundle)) {
            return SignInBody.withGoogleToken(bundle.getString(GOOGLE_TOKEN_EXTRA), oAuth.getClientId(), oAuth.getClientSecret());
        } else {
            throw new IllegalArgumentException("invalid param " + bundle);
        }
    }

    private boolean isFromFacebook(Bundle data) {
        return data.containsKey(FACEBOOK_TOKEN_EXTRA);
    }

    private boolean isFromGoogle(Bundle bundle) {
        return bundle.containsKey(GOOGLE_TOKEN_EXTRA);
    }

    private boolean isFromUserCredentials(Bundle data) {
        return data.containsKey(USERNAME_EXTRA)
                && data.containsKey(PASSWORD_EXTRA);
    }

    private String getUsername(Bundle data) {
        return data.getString(USERNAME_EXTRA);
    }

    private String getPassword(Bundle data) {
        return data.getString(PASSWORD_EXTRA);
    }
}
