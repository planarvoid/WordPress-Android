package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.storage.UserStorage;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.Log;
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

    private static final String TAG = SignupTask.class.getSimpleName();
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    private TokenInformationGenerator mTokenInformationGenerator;
    private PublicCloudAPI mOldCloudAPI;

    protected SignupTask(SoundCloudApplication app, TokenInformationGenerator tokenInformationGenerator,
                         UserStorage userStorage, PublicCloudAPI oldCloudAPI) {
        super(app, userStorage);
        mTokenInformationGenerator = tokenInformationGenerator;
        mOldCloudAPI = oldCloudAPI;
    }

    public SignupTask(SoundCloudApplication soundCloudApplication){
        this(soundCloudApplication, new TokenInformationGenerator(new PublicApi(soundCloudApplication)),
                new UserStorage(), new PublicApi(soundCloudApplication));
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        final SoundCloudApplication app = getSoundCloudApplication();

        AuthTaskResult result = doSignup(app, params[0]);
        if (result.wasSuccess()){
            // do token exchange
            Token token;
            try {
                token = mTokenInformationGenerator.getToken(params[0]);
                if (token == null || !app.addUserAccountAndEnableSync(result.getUser(), token, SignupVia.API)) {
                    return AuthTaskResult.failure(app.getString(R.string.authentication_signup_error_message));
                }
            } catch (IOException e) {
                return AuthTaskResult.failure(e);
            }
        }
        return result;
    }

    protected AuthTaskResult doSignup(SoundCloudApplication app, Bundle params){
        try {
            // explicitly request signup scope
            final Token signup = mOldCloudAPI.clientCredentials(Token.SCOPE_SIGNUP);

            Log.d(TAG, signup.toString());

            HttpResponse resp = mOldCloudAPI.post(Request.to(Endpoints.USERS).with(
                    Params.User.EMAIL, params.getString(KEY_USERNAME),
                    Params.User.PASSWORD, params.getString(KEY_PASSWORD),
                    Params.User.PASSWORD_CONFIRMATION, params.getString(KEY_PASSWORD),
                    Params.User.TERMS_OF_USE, "1"
            ).usingToken(signup));

            int statusCode = resp.getStatusLine().getStatusCode();

            switch (statusCode) {
                case HttpStatus.SC_CREATED: // success case
                    final User user = mOldCloudAPI.getMapper().readValue(resp.getEntity().getContent(), User.class);
                    return AuthTaskResult.success(user,SignupVia.API);

                case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                    return AuthTaskResult.failure(extractErrors(resp));

                case HttpStatus.SC_FORBIDDEN:
                    // most likely did not have valid signup scope at this point, but make sure before
                    Header wwwAuth = resp.getFirstHeader("WWW-Authenticate");
                    if (wwwAuth != null && wwwAuth.getValue().contains("insufficient_scope")) {
                        return AuthTaskResult.failure(app.getString(R.string.signup_scope_revoked));
                    } else {
                        return AuthTaskResult.failure(app.getString(R.string.authentication_signup_error_message));
                    }

                default:
                    if (statusCode >= 500) {
                        return AuthTaskResult.failure(app.getString(R.string.error_server_problems_message));
                    } else {
                        return AuthTaskResult.failure(app.getString(R.string.authentication_signup_error_message));
                    }
            }
        } catch (CloudAPI.InvalidTokenException e){
            return AuthTaskResult.failure(app.getString(R.string.signup_scope_revoked));
        } catch (IOException e) {
            return AuthTaskResult.failure(e);
        }
    }
}
