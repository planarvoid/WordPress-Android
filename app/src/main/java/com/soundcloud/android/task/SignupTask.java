package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.model.User;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import android.util.Log;

import java.io.IOException;

public class SignupTask extends AsyncApiTask<String, Void, User>  {
    public SignupTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected User doInBackground(String... params) {
        final String email = params[0];
        final String password = params[1];
        try {
            // explicitly request signup scope
            final Token signup = mApi.clientCredentials(Token.SCOPE_SIGNUP);

            HttpResponse resp = mApi.post(Request.to(USERS).with(
                    Params.User.EMAIL, email,
                    Params.User.PASSWORD, password,
                    Params.User.PASSWORD_CONFIRMATION, password,
                    Params.User.TERMS_OF_USE, "1"
            ).usingToken(signup));

            switch (resp.getStatusLine().getStatusCode()) {
                case SC_CREATED:
                    return mApi.getMapper().readValue(resp.getEntity().getContent(), User.class);
                case SC_UNPROCESSABLE_ENTITY:
                    extractErrors(resp);
                    break;
                case SC_FORBIDDEN:
                    // most likely did not have valid signup scope at this point, but make sure before
                    // showing error
                    Header wwwAuth = resp.getFirstHeader("WWW-Authenticate");
                    if (wwwAuth != null && wwwAuth.getValue().contains("insufficient_scope")) {
                        mErrors.add(mApi.getContext().getString(R.string.signup_scope_revoked));
                    }
                    break;
            }
            warn("invalid response", resp);
            return null;
        } catch (CloudAPI.InvalidTokenException e) {
            warn("error creating user - invalid scope", e);
            mErrors.add(mApi.getContext().getString(R.string.signup_scope_revoked));
            return null;
        } catch (IOException e) {
            warn("error creating user", e);
            return null;
        }
    }
}
