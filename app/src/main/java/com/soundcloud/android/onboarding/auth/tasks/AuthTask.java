package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.onboarding.auth.AuthTaskFragment;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.tasks.ParallelAsyncTask;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.Collections;

public abstract class AuthTask extends ParallelAsyncTask<Bundle, Void, LegacyAuthTaskResult> {

    private static final int ME_SYNC_DELAY_MILLIS = 30 * 1000;

    private final SoundCloudApplication app;
    private final StoreUsersCommand storeUsersCommand;
    private final SyncInitiatorBridge syncInitiatorBridge;
    private AuthTaskFragment fragment;

    public AuthTask(SoundCloudApplication application,
                    StoreUsersCommand storeUsersCommand,
                    SyncInitiatorBridge syncInitiatorBridge) {
        this.app = application;
        this.storeUsersCommand = storeUsersCommand;
        this.syncInitiatorBridge = syncInitiatorBridge;
    }

    public void setTaskOwner(AuthTaskFragment taskOwner) {
        fragment = taskOwner;
    }

    protected SoundCloudApplication getSoundCloudApplication() {
        return app;
    }

    protected String getString(int resourceId) {
        return app.getString(resourceId);
    }

    @Override
    protected void onPostExecute(LegacyAuthTaskResult result) {
        if (fragment == null) {
            return;
        }
        fragment.onTaskResult(result);
    }

    protected Boolean addAccount(ApiUser user, Token token, SignupVia via) {
        if (app.addUserAccountAndEnableSync(user, token, via)) {
            storeUsersCommand.call(Collections.singleton(user));
            if (via != SignupVia.NONE) {
                // user has signed up, schedule sync of user data to possibly refresh image data
                // which gets processed asynchronously by the backend and is only available after signup has happened
                new Handler(Looper.getMainLooper()).postDelayed(() -> syncInitiatorBridge.refreshMe(), ME_SYNC_DELAY_MILLIS);
            }
            return true;
        } else {
            return false;
        }
    }
}
