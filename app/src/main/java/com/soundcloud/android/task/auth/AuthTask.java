package com.soundcloud.android.task.auth;

import com.fasterxml.jackson.databind.ObjectReader;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.dialog.auth.AuthTaskFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.utils.IOUtils;
import org.apache.http.HttpResponse;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.List;

public abstract class AuthTask extends AsyncTask<Bundle, Void, AuthTask.Result>{
    public static final int ME_SYNC_DELAY_MILLIS = 30 * 1000;
    private final SoundCloudApplication mApp;
    private AuthTaskFragment mFragment;

    protected AuthTask(SoundCloudApplication application) {
        mApp = application;
    }

    public void setFragment(AuthTaskFragment fragment)
    {
        mFragment = fragment;
    }

    protected SoundCloudApplication getSoundCloudApplication() {
        return mApp;
    }

    @Override
    protected void onPostExecute(Result result)
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

    protected AuthorizationException extractErrors(HttpResponse resp) throws IOException {
        final ObjectReader reader = getSoundCloudApplication().getMapper().reader();
        final List<String> errors = IOUtils.parseError(reader, resp.getEntity().getContent());
        return new AuthorizationException(errors);
    }

    public static class Result {
        private boolean     success;
        private User        user;
        private SignupVia   signupVia;
        private Exception   exception;

        public Result(User user, SignupVia signupVia) {
            success = true;
            this.user = user;
            this.signupVia = signupVia;
        }

        public Result(Exception e){
            exception = e;
            success = false;
        }

        public boolean wasSuccess() {
            return success;
        }

        public User getUser() {
            return user;
        }

        public SignupVia getSignupVia() {
            return signupVia;
        }

        public Exception getException() {
            return exception;
        }

        public String[] getErrors(){
            return exception instanceof AuthorizationException ? ((AuthorizationException) exception).getErrors() : null;
        }
    }



}
