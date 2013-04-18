package com.soundcloud.android.task.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.activity.auth.TokenUtil;
import com.soundcloud.android.model.User;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.apache.http.Header;
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

        Result result = doSignup(app, params[0]);
        if (result.wasSuccess()){
            // do token exchange
            Token token;
            try {
                token = TokenUtil.getToken(getSoundCloudApplication(), params[0]);
                if (token == null || !app.addUserAccountAndEnableSync(result.getUser(), token, SignupVia.API)) {
                    return new Result(new AuthorizationException(app.getString(R.string.authentication_signup_error_message)));
                }
            } catch (IOException e) {
                return new Result(e);
            }
        }
        return result;
    }

    protected Result doSignup(SoundCloudApplication app, Bundle params){
        User user = null;
        try {
            // explicitly request signup scope
            final Token signup = app.clientCredentials(Token.SCOPE_SIGNUP);

            HttpResponse resp = app.post(Request.to(Endpoints.USERS).with(
                    Params.User.EMAIL, params.getString(KEY_USERNAME),
                    Params.User.PASSWORD, params.getString(KEY_PASSWORD),
                    Params.User.PASSWORD_CONFIRMATION, params.getString(KEY_PASSWORD),
                    Params.User.TERMS_OF_USE, "1"
            ).usingToken(signup));

            int statusCode = resp.getStatusLine().getStatusCode();
            switch (statusCode) {
                case HttpStatus.SC_CREATED:
                    user = app.getMapper().readValue(resp.getEntity().getContent(), User.class);
                    break;

                case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                    return new Result(extractErrors(resp));

                case HttpStatus.SC_FORBIDDEN:
                    // most likely did not have valid signup scope at this point, but make sure before
                    // showing error
                    Header wwwAuth = resp.getFirstHeader("WWW-Authenticate");
                    if (wwwAuth != null && wwwAuth.getValue().contains("insufficient_scope")) {
                        return new Result(new AuthorizationException(app.getString(R.string.signup_scope_revoked)));
                    }
                    break;

                default:
                    if (statusCode >= 500) {
                        return new Result(new AuthorizationException(app.getString(R.string.error_server_problems_message)));
                    } else {
                        return new Result(new AuthorizationException(app.getString(R.string.authentication_signup_error_message)));
                    }
            }
        } catch (CloudAPI.InvalidTokenException e){
            return new Result(new AuthorizationException(app.getString(R.string.signup_scope_revoked)));
        } catch (IOException e) {
            return new Result(e);
        }
        return new Result(user,SignupVia.API);
    }
}
