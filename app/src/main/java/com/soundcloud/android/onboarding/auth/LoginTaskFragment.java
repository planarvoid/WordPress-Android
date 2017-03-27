package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.onboarding.auth.tasks.LoginTask;
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
        return new LoginTask((SoundCloudApplication) getActivity().getApplication(),
                             storeUsersCommand,
                             accountOperations,
                             syncInitiatorBridge,
                             signInOperations);
    }

    @Override
    protected String getErrorFromResult(Activity activity, AuthTaskResult result) {
        if (result.wasUnauthorized()) {
            return activity.getString(R.string.authentication_login_error_password_message);
        } else {
            return super.getErrorFromResult(activity, result);
        }
    }

    public interface Factory {

        Factory DEFAULT = new Factory() {
            @Override
            public LoginTaskFragment create(String username, String password) {
                return LoginTaskFragment.create(username, password);
            }

            @Override
            public LoginTaskFragment create(Bundle bundle) {
                return LoginTaskFragment.create(bundle);
            }
        };

        LoginTaskFragment create(String username, String password);

        LoginTaskFragment create(Bundle bundle);
    }
}
