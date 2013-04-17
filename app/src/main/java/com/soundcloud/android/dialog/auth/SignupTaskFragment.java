package com.soundcloud.android.dialog.auth;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.task.auth.AuthTask;
import com.soundcloud.android.task.auth.SignupTask;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;

public class SignupTaskFragment extends AuthTaskFragment {

    public static SignupTaskFragment create(String username, String password){
        Bundle b = new Bundle();
        b.putString(SignupTask.KEY_USERNAME, username);
        b.putString(SignupTask.KEY_PASSWORD, password);

        SignupTaskFragment signupTaskFragment = new SignupTaskFragment();
        signupTaskFragment.setArguments(b);
        return signupTaskFragment;
    }

    @NotNull
    @Override
    AuthTask createAuthTask() {
        return new SignupTask((SoundCloudApplication) getActivity().getApplication());
    }

    @Override
    Bundle getTaskParams() {
        return getArguments();
    }
}
