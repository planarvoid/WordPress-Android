package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.onboarding.auth.tasks.AddUserInfoTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.api.CloudAPI;
import org.jetbrains.annotations.NotNull;

import android.accounts.Account;
import android.app.Activity;
import android.os.Bundle;

import javax.inject.Inject;
import java.io.File;

public class AddUserInfoTaskFragment extends AuthTaskFragment {

    private static final String USERNAME_EXTRA = "username";
    private static final String AVATAR_EXTRA = "avatar";

    @Inject ApiClient apiClient;
    @Inject LegacyUserStorage userStorage;

    public static AddUserInfoTaskFragment create(String username, File avatarFile) {
        final Bundle param = new Bundle();
        param.putString(USERNAME_EXTRA, username);
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
        final SoundCloudApplication application = (SoundCloudApplication) getActivity().getApplication();
        final Account account = accountOperations.getSoundCloudAccount();
        final String username = getArguments().getString(USERNAME_EXTRA);
        final String permalink = account != null ? account.name : username;
        final File avatarFile = getArguments().containsKey(AVATAR_EXTRA) ? new File(getArguments().getString(AVATAR_EXTRA)) : null;
        return new AddUserInfoTask(application, permalink, username, avatarFile, userStorage, apiClient, accountOperations);
    }

    @Override
    protected String getErrorFromResult(Activity activity, AuthTaskResult result) {
        final Exception exception = result.getException();
        if (isLoginCredentialsException(exception)) {
            return activity.getString(R.string.authentication_login_error_password_message);
        } else {
            return super.getErrorFromResult(activity, result);
        }
    }

    private boolean isLoginCredentialsException(Exception exception) {
        return exception instanceof CloudAPI.InvalidTokenException || exception instanceof TokenRetrievalException;
    }
}
