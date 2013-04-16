package com.soundcloud.android.task.auth;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.dialog.auth.AuthTaskFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

public abstract class AuthTask extends AsyncTask<Bundle, Void, AuthTask.Result>{

    AuthTaskFragment mFragment;
    public void setFragment(AuthTaskFragment fragment)
    {
        mFragment = fragment;
    }

    @Override
    protected void onPostExecute(Result result)
    {
        if (mFragment == null) return;
        mFragment.onTaskResult(result);
    }

    protected static Boolean addAccount(final SoundCloudApplication app, User user, SignupVia via) {
        boolean accountCreated = app.addUserAccountAndEnableSync(user, app.getToken(), via);
        if (accountCreated) {
            new UserStorage(app).createOrUpdate(user);
            if (via != SignupVia.NONE) {
                // user has signed up, schedule sync of user data to possibly refresh image data
                // which gets processed asynchronously by the backend and is only available after signup has happened
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        app.startService(new Intent(app, ApiSyncService.class).setData(Content.ME.uri));
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
    }



}
