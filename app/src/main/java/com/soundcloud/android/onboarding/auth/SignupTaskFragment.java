package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.SignupTask;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;

public class SignupTaskFragment extends AuthTaskFragment {

    public static Bundle getParams(String username, String password){
        Bundle b = new Bundle();
        b.putString(SignupTask.KEY_USERNAME, username);
        b.putString(SignupTask.KEY_PASSWORD, password);
        return b;
    }

    public static SignupTaskFragment create(Bundle params){
        SignupTaskFragment signupTaskFragment = new SignupTaskFragment();
        signupTaskFragment.setArguments(params);
        return signupTaskFragment;
    }

    @NotNull
    @Override
    AuthTask createAuthTask() {
        return new SignupTask((SoundCloudApplication) getActivity().getApplication());
    }

}
