package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.SignupTask;
import com.soundcloud.android.profile.BirthdayInfo;

import org.jetbrains.annotations.NotNull;

import android.os.Bundle;

public class SignupTaskFragment extends AuthTaskFragment {

    public static Bundle getParams(String username, String password, BirthdayInfo birthday, String gender){
        Bundle b = new Bundle();
        b.putString(SignupTask.KEY_USERNAME, username);
        b.putString(SignupTask.KEY_PASSWORD, password);
        b.putSerializable(SignupTask.KEY_BIRTHDAY, birthday);
        b.putString(SignupTask.KEY_GENDER, gender);
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
