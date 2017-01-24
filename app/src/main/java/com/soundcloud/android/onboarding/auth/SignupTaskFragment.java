package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.LegacyAuthTaskResult;
import com.soundcloud.android.onboarding.auth.tasks.LoginTask;
import com.soundcloud.android.onboarding.auth.tasks.SignupTask;
import com.soundcloud.android.profile.BirthdayInfo;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;
import android.os.Handler;

import java.util.concurrent.TimeUnit;

public class SignupTaskFragment extends AuthTaskFragment {

    private static final long DELAY_BEFORE_RETRY = TimeUnit.SECONDS.toMillis(5);
    private static final int MAX_LOGIN_RETRY = 3;

    private final Handler handler = new Handler();
    private final Runnable loginOperation = () -> getLoginTask().executeOnThreadPool(getArguments());

    private int remainingLoginTries = 0;

    public static Bundle getParams(String username, String password, BirthdayInfo birthday, String gender) {
        Bundle b = new Bundle();
        b.putString(SignupTask.KEY_USERNAME, username);
        b.putString(SignupTask.KEY_PASSWORD, password);
        b.putSerializable(SignupTask.KEY_BIRTHDAY, birthday);
        b.putString(SignupTask.KEY_GENDER, gender);
        return b;
    }

    public static SignupTaskFragment create(Bundle params) {
        SignupTaskFragment signupTaskFragment = new SignupTaskFragment();
        signupTaskFragment.setArguments(params);
        return signupTaskFragment;
    }

    @Override
    public void onTaskResult(LegacyAuthTaskResult result) {
        if (result.wasSignUpFailedToLogin()) {
            setRetryToLogin(MAX_LOGIN_RETRY);
        }

        if (shouldRetryLogin(result)) {
            retryToLogin();
        } else {
            setRetryToLogin(0);
            super.onTaskResult(result);
        }
    }

    private void setRetryToLogin(int maxRetry) {
        remainingLoginTries = maxRetry;
    }

    private void retryToLogin() {
        remainingLoginTries--;
        handler.postDelayed(loginOperation, DELAY_BEFORE_RETRY);
    }

    private boolean shouldRetryLogin(LegacyAuthTaskResult result) {
        return !result.wasSuccess() && remainingLoginTries > 0;
    }

    private LoginTask getLoginTask() {
        final LoginTask loginTask = new LoginTask((SoundCloudApplication) getActivity().getApplication(),
                                                  tokenUtils,
                                                  storeUsersCommand,
                                                  configurationOperations,
                                                  eventBus,
                                                  accountOperations,
                                                  apiClient,
                                                  syncInitiatorBridge);
        loginTask.setTaskOwner(this);
        return loginTask;
    }

    @NotNull
    @Override
    AuthTask createAuthTask() {
        return new SignupTask((SoundCloudApplication) getActivity().getApplication(),
                              storeUsersCommand,
                              tokenUtils,
                              apiClient,
                              syncInitiatorBridge,
                              featureFlags,
                              signUpOperations);
    }
}
