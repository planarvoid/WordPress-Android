package com.soundcloud.android.dialog.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.task.auth.AuthTask;
import com.soundcloud.android.task.auth.LoginTask;
import com.soundcloud.api.CloudAPI;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;

public class LoginTaskFragment extends AuthTaskFragment {
    @NotNull
    @Override
    AuthTask createAuthTask() {
        return new LoginTask((SoundCloudApplication) getActivity().getApplication());
    }

    @Override
    String getErrorFromResult(Activity activity, AuthTask.Result result) {
        final Exception exception = result.getException();
        int messageId;
        if (exception instanceof CloudAPI.InvalidTokenException) {
            messageId = R.string.authentication_login_error_password_message;

        } else if (exception instanceof CloudAPI.ApiResponseException
                && ((CloudAPI.ApiResponseException) exception).getStatusCode() >= 400) {
            messageId = R.string.error_server_problems_message;
        } else {
            messageId = R.string.authentication_error_no_connection_message;
        }
        return activity.getString(messageId);
    }
}
