package com.soundcloud.android.task.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.AbstractLoginActivity;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.dialog.auth.AuthTaskFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.api.Token;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

public abstract class AuthTask extends AsyncTask<Bundle, Void, AuthTask.Result>{
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

    protected Token getTokens(Bundle param) throws IOException {
        final String[] scopes = param.getStringArray(AbstractLoginActivity.SCOPES_EXTRA);

        if (param.containsKey(AbstractLoginActivity.CODE_EXTRA)) {
            return mApp.authorizationCode(param.getString(AbstractLoginActivity.CODE_EXTRA), scopes);

        } else if (param.containsKey(AbstractLoginActivity.USERNAME_EXTRA)
                && param.containsKey(AbstractLoginActivity.PASSWORD_EXTRA)) {
            return mApp.login(param.getString(AbstractLoginActivity.USERNAME_EXTRA),
                    param.getString(AbstractLoginActivity.PASSWORD_EXTRA), scopes);

        } else if (param.containsKey(AbstractLoginActivity.EXTENSION_GRANT_TYPE_EXTRA)) {
            return mApp.extensionGrantType(param.getString(AbstractLoginActivity.EXTENSION_GRANT_TYPE_EXTRA), scopes);

        } else {
            throw new IllegalArgumentException("invalid param " + param);
        }
    }

    protected Boolean addAccount(User user, SignupVia via) {
        boolean accountCreated = mApp.addUserAccountAndEnableSync(user, mApp.getToken(), via);
        if (accountCreated) {
            new UserStorage(mApp).createOrUpdate(user);
            if (via != SignupVia.NONE) {
                // user has signed up, schedule sync of user data to possibly refresh image data
                // which gets processed asynchronously by the backend and is only available after signup has happened
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mApp.startService(new Intent(mApp, ApiSyncService.class).setData(Content.ME.uri));
                    }
                }, 30 * 1000);
            }
            return true;
        } else {
            return false;
        }
    }

    public class Result {
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
