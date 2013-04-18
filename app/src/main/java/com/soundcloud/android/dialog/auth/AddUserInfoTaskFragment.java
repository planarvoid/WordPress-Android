package com.soundcloud.android.dialog.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.auth.AddUserInfoTask;
import com.soundcloud.android.task.auth.AuthTask;
import com.soundcloud.android.task.auth.LoginTask;
import com.soundcloud.api.CloudAPI;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.os.Bundle;

import java.io.File;

public class AddUserInfoTaskFragment extends AuthTaskFragment {

    public static final String USER_EXTRA = "user";
    public static final String AVATAR_EXTRA = "avatar";

    public static AddUserInfoTaskFragment create(User updatedUser, File avatarFile) {
        final Bundle param = new Bundle();
        param.putParcelable(USER_EXTRA,updatedUser);
        if (avatarFile != null && avatarFile.exists()){
            param.putSerializable(AVATAR_EXTRA, avatarFile.getAbsolutePath());
        }

        AddUserInfoTaskFragment fragment = new AddUserInfoTaskFragment();
        fragment.setArguments(param);
        return fragment;
    }

    @NotNull
    @Override
    AuthTask createAuthTask() {
        return new AddUserInfoTask((SoundCloudApplication) getActivity().getApplication(),
                (User) getArguments().getParcelable(USER_EXTRA),
                getArguments().containsKey(AVATAR_EXTRA) ? new File(getArguments().getString(AVATAR_EXTRA)) : null
        );
    }

    @Override
    Bundle getTaskParams() {
        return getArguments();
    }

    @Override
    protected String getErrorFromResult(Activity activity, AuthTask.Result result) {
        final Exception exception = result.getException();
        if (exception instanceof CloudAPI.InvalidTokenException) {
            return activity.getString(R.string.authentication_login_error_password_message);
        } else {
            return super.getErrorFromResult(activity, result);
        }
    }
}
