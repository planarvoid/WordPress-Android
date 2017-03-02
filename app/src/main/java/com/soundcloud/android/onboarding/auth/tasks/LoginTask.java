package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;

public class LoginTask extends AuthTask {

    protected final AccountOperations accountOperations;
    private final SignInOperations signInOperations;

    public LoginTask(@NotNull SoundCloudApplication application,
                     StoreUsersCommand storeUsersCommand,
                     AccountOperations accountOperations,
                     SyncInitiatorBridge syncInitiatorBridge,
                     SignInOperations signInOperations) {
        super(application, storeUsersCommand, syncInitiatorBridge);
        this.accountOperations = accountOperations;
        this.signInOperations = signInOperations;
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        return signInOperations.signIn(params[0]);
    }
}
