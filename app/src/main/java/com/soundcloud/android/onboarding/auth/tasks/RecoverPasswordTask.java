package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.legacy.AsyncApiTask;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.api.legacy.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;

public class RecoverPasswordTask extends AsyncApiTask<String, Void, Boolean> {
    public RecoverPasswordTask(PublicApiWrapper api) {
        super(api);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        final String email = params[0];
        try {
            final Token signup = api.clientCredentials();
            HttpResponse resp = api.post(
                    Request.to(SEND_PASSWORD).with("email", email).usingToken(signup));
            final int code = resp.getStatusLine().getStatusCode();

            switch (code) {
                case HttpStatus.SC_ACCEPTED: return true;
                case HttpStatus.SC_NOT_FOUND:
                    extractErrors(resp);
                default:
                    warn("unexpected status code "+code+" received");
                    return false;
            }
        } catch (IOException e) {
            warn("error requesting password reset", e);
            return false;
        }
    }
}
