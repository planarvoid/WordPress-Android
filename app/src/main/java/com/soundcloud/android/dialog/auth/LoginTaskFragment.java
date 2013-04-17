package com.soundcloud.android.dialog.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.task.auth.AuthTask;
import com.soundcloud.android.task.auth.LoginTask;
import com.soundcloud.api.CloudAPI;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.os.Bundle;

public class LoginTaskFragment extends AuthTaskFragment {

    public static final String USERNAME_EXTRA = "username";
    public static final String PASSWORD_EXTRA = "password";

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
        return new LoginTask((SoundCloudApplication) getActivity().getApplication());
    }

    @Override
    Bundle getTaskParams() {
        return getArguments();
    }

    @Override
    String getErrorFromResult(Activity activity, AuthTask.Result result) {
        final Exception exception = result.getException();
        int messageId;
        if (exception instanceof CloudAPI.InvalidTokenException) {
            messageId = R.string.authentication_login_error_password_message;
        } else if (exception instanceof CloudAPI.ApiResponseException) {
            messageId = R.string.error_server_problems_message;
        } else {
            messageId = R.string.authentication_error_no_connection_message;
        }
        return activity.getString(messageId);
    }
}
