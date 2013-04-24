package com.soundcloud.android.task.auth;

import com.fasterxml.jackson.databind.ObjectReader;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.dialog.auth.AuthTaskFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.ParallelAsyncTask;
import com.soundcloud.android.utils.IOUtils;
import org.apache.http.HttpResponse;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public abstract class AuthTask extends ParallelAsyncTask<Bundle, Void, AuthTaskResult> {

    private static final int ME_SYNC_DELAY_MILLIS = 30 * 1000;

    private final SoundCloudApplication mApp;
    private AuthTaskFragment mFragment;

    public AuthTask(SoundCloudApplication application) {
        mApp = application;
    }

    public void setTaskOwner(AuthTaskFragment taskOwner) {
        mFragment = taskOwner;
    }

    protected SoundCloudApplication getSoundCloudApplication() {
        return mApp;
    }

    @Override
    protected void onPostExecute(AuthTaskResult result)
    {
        if (mFragment == null) return;
        mFragment.onTaskResult(result);
    }

    protected Boolean addAccount(User user, SignupVia via) {
        boolean accountCreated = mApp.addUserAccountAndEnableSync(user, mApp.getToken(), via);
        if (accountCreated) {
            new UserStorage(mApp).createOrUpdate(user);
            if (via != SignupVia.NONE) {
                // user has signed up, schedule sync of user data to possibly refresh image data
                // which gets processed asynchronously by the backend and is only available after signup has happened
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mApp.startService(new Intent(mApp, ApiSyncService.class).setData(Content.ME.uri));
                    }
                }, ME_SYNC_DELAY_MILLIS);
            }
            return true;
        } else {
            return false;
        }
    }

    protected AuthTaskException extractErrors(HttpResponse resp) throws IOException {
        final ObjectReader reader = getSoundCloudApplication().getMapper().reader();
        final List<String> errors = IOUtils.parseError(reader, resp.getEntity().getContent());
        return new AuthTaskException(errors);
    }
}
