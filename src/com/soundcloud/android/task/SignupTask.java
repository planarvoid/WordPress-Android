package com.soundcloud.android.task;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.objects.User;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.apache.http.HttpResponse;

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
            final Token signup = mApi.clientCredentials();
            HttpResponse resp = mApi.post(Request.to(USERS).with(
                    Params.User.EMAIL, email,
                    Params.User.PASSWORD, password,
                    Params.User.PASSWORD_CONFIRMATION, password,
                    Params.User.TERMS_OF_USE, "1"
            ).usingToken(signup));

            final int code = resp.getStatusLine().getStatusCode();
            switch (code) {
                case SC_CREATED:
                    return mApi.getMapper().readValue(resp.getEntity().getContent(), User.class);
                case SC_UNPROCESSABLE_ENTITY:
                    extractErrors(resp);
                default:
                    warn("invalid response", resp);
                    return null;
            }
        } catch (IOException e) {
            warn("error creating user", e);
            return null;
        }
    }
}
