package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.onboarding.auth.SignUpOperations;
import com.soundcloud.android.sync.SyncInitiatorBridge;

import android.os.Bundle;

public class SignupTask extends AuthTask {
    private final SignUpOperations signUpOperations;

    public SignupTask(SoundCloudApplication soundCloudApplication,
                      StoreUsersCommand storeUsersCommand,
                      SyncInitiatorBridge syncInitiatorBridge,
                      SignUpOperations signUpOperations) {
        super(soundCloudApplication, storeUsersCommand, syncInitiatorBridge);
        this.signUpOperations = signUpOperations;
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
            return signUpOperations.signUp(params[0]);
    }
}
