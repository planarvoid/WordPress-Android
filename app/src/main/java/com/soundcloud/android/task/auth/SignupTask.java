package com.soundcloud.android.task.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.Wrapper;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.model.User;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.Bundle;

import java.io.IOException;

public class SignupTask extends AuthTask {

    public static String KEY_USERNAME = "username";
    public static String KEY_PASSWORD = "password";

    public SignupTask(SoundCloudApplication app) {
        super(app);
    }

    @Override
    protected Result doInBackground(Bundle... params) {
        final SoundCloudApplication app = getSoundCloudApplication();
        User user = null;
        try {
            // explicitly request signup scope
            final Token signup = app.clientCredentials(Token.SCOPE_SIGNUP);

            HttpResponse resp = app.post(Request.to(Endpoints.USERS).with(
                    Params.User.EMAIL, params[0].getString(KEY_USERNAME),
                    Params.User.PASSWORD, params[0].getString(KEY_PASSWORD),
                    Params.User.PASSWORD_CONFIRMATION, params[0].getString(KEY_PASSWORD),
                    Params.User.TERMS_OF_USE, "1"
            ).usingToken(signup));

            int statusCode = resp.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_CREATED){
                user = app.getMapper().readValue(resp.getEntity().getContent(), User.class);
            } else if (Wrapper.isStatusCodeServerError(statusCode)){
                return new Result(new AuthorizationException(R.string.error_server_problems_message));
            } else {
                return new Result(new AuthorizationException(R.string.authentication_signup_error_message));
            }

        } catch (IOException e) {
            return new Result(e);
        }

        Token token;
        try {
            token = getTokens(params[0]);
            if (token == null || !app.addUserAccountAndEnableSync(user, token, SignupVia.API)) {
                return new Result(new AuthorizationException(R.string.authentication_signup_error_message));
            }
        } catch (IOException e) {
            return new Result(e);
        }

        //writeNewSignupToLog();
        return new Result(user, SignupVia.API);


    }


}
