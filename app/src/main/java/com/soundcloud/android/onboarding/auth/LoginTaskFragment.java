package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.onboarding.auth.tasks.LoginTask;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.CloudAPI;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.os.Bundle;

public class LoginTaskFragment extends AuthTaskFragment {

    private static final String USERNAME_EXTRA = "username";
    private static final String PASSWORD_EXTRA = "password";

    public static LoginTaskFragment create(String username, String password) {
        final Bundle param = new Bundle();
        param.putString(USERNAME_EXTRA, username);
        param.putString(PASSWORD_EXTRA, password);
        return create(param);
    }

    public static LoginTaskFragment create(Bundle param) {
        LoginTaskFragment fragment = new LoginTaskFragment();
        fragment.setArguments(param);
        return fragment;
    }

    @NotNull
    @Override
    AuthTask createAuthTask() {
        Log.w(Log.ONBOARDING_TAG, "creating login auth task");
        return new LoginTask((SoundCloudApplication)getActivity().getApplication(), configurationOperations, eventBus, accountOperations);
    }

    @Override
    protected String getErrorFromResult(Activity activity, AuthTaskResult result) {
        final Exception exception = result.getException();
        Log.w(Log.ONBOARDING_TAG, "email login error with " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        if (exception instanceof CloudAPI.InvalidTokenException) {
            return activity.getString(R.string.authentication_login_error_password_message);
        } else {
            return super.getErrorFromResult(activity, result);
        }
    }
}
