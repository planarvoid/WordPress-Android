package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.onboarding.auth.AuthTaskFragment;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.tasks.ParallelAsyncTask;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public abstract class AuthTask extends ParallelAsyncTask<Bundle, Void, AuthTaskResult> {

    private static final int ME_SYNC_DELAY_MILLIS = 30 * 1000;

    private final SoundCloudApplication app;
    private final LegacyUserStorage userStorage;
    private AuthTaskFragment fragment;

    public AuthTask(SoundCloudApplication application, LegacyUserStorage userStorage) {
        this.app = application;
        this.userStorage = userStorage;
    }

    public void setTaskOwner(AuthTaskFragment taskOwner) {
        fragment = taskOwner;
    }

    protected SoundCloudApplication getSoundCloudApplication() {
        return app;
    }

    @Override
    protected void onPostExecute(AuthTaskResult result) {
        if (fragment == null) {
            return;
        }
        fragment.onTaskResult(result);
    }

    protected Boolean addAccount(PublicApiUser user, Token token, SignupVia via) {
        if (app.addUserAccountAndEnableSync(user, token, via)) {
            userStorage.createOrUpdate(user);
            if (via != SignupVia.NONE) {
                // user has signed up, schedule sync of user data to possibly refresh image data
                // which gets processed asynchronously by the backend and is only available after signup has happened
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        app.startService(new Intent(app, ApiSyncService.class).setData(Content.ME.uri));
                    }
                }, ME_SYNC_DELAY_MILLIS);
            }
            return true;
        } else {
            return false;
        }
    }
}
