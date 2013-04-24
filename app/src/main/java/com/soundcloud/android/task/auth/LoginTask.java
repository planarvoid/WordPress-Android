package com.soundcloud.android.task.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.activity.auth.TokenUtil;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;

import java.io.IOException;

public class LoginTask extends AuthTask {

    private TokenUtil tokenUtils;
    private FetchUserTask fetchUserTask;

    public LoginTask(@NotNull SoundCloudApplication application, TokenUtil tokenUtils, FetchUserTask fetchUserTask) {
        super(application);
        this.tokenUtils = tokenUtils;
        this.fetchUserTask = fetchUserTask;
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        return login(params[0]);
    }

    protected AuthTaskResult login(Bundle data) {
        SoundCloudApplication app = getSoundCloudApplication();

        try {
            Token token = tokenUtils.getToken(app, TokenUtil.configureDefaultScopeExtra(data));
            Log.d("LoginTask[Token](" + token + ")");

            final User user = fetchUserTask.resolve(Request.to(Endpoints.MY_DETAILS));
            if (user == null) {
                return AuthTaskResult.failure(app.getString(R.string.authentication_error_no_connection_message));
            }
            Log.d("LoginTask[User](" + user + ")");

            SignupVia signupVia = token.getSignup() != null ? SignupVia.fromString(token.getSignup()) : SignupVia.NONE;
            if (!addAccount(user, signupVia)) {
                // might mean the account already existed or an unknown failure adding account.
                // this should never happen, just show a generic error message
                return AuthTaskResult.failure(app.getString(R.string.authentication_login_error_message));
            }

            return AuthTaskResult.success(user, signupVia);

        } catch (IOException e) {
            Log.e("Error retrieving SC API token" + e.getMessage());
            return AuthTaskResult.failure(e);
        }
    }
}
