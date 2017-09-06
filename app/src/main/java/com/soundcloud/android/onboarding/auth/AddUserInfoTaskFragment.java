package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.onboarding.auth.tasks.AddUserInfoTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;

import android.accounts.Account;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.io.File;

public class AddUserInfoTaskFragment extends AuthTaskFragment {

    private static final String USERNAME_EXTRA = "username";
    private static final String AVATAR_EXTRA = "avatar";

    public static AddUserInfoTaskFragment create(String username, @Nullable File avatarFile) {
        final Bundle param = new Bundle();
        param.putString(USERNAME_EXTRA, username);
        if (avatarFile != null && avatarFile.exists() && avatarFile.length() > 0) {
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
        final Optional<Account> account = accountOperations.getSoundCloudAccount();
        final String username = getArguments().getString(USERNAME_EXTRA);
        final String permalink = account.transform(acct -> acct.name).or(username);
        final String avatarFilename = getArguments().getString(AVATAR_EXTRA, null);
        final File avatarFile = avatarFilename != null ? new File(avatarFilename) : null;

        return new AddUserInfoTask(application,
                                   permalink,
                                   username,
                                   avatarFile,
                                   storeUsersCommand,
                                   apiClient,
                                   accountOperations,
                                   syncInitiatorBridge);
    }
}
