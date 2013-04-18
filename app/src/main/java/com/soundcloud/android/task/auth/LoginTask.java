package com.soundcloud.android.task.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
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
    public static final String[] SCOPES_TO_REQUEST = { Token.SCOPE_NON_EXPIRING };
    public static final String SCOPES_EXTRA = "scopes";

    public LoginTask(@NotNull SoundCloudApplication application) {
        super(application);
    }

    @Override
    protected Result doInBackground(Bundle... params) {
        return login(params[0]);
    }

    protected Result login(Bundle data) {
        if (!data.containsKey(SCOPES_EXTRA)) {
            // default to non-expiring scope
            data.putStringArray(SCOPES_EXTRA, SCOPES_TO_REQUEST);
        }

        Token token;
        try {
            token = getTokens(data);
        } catch (IOException e) {
            Log.e("Error retrieving SC API token" + e.getMessage());
            return new Result(e);
        }
        Log.d("LoginTask[Token](" + token + ")");

        SoundCloudApplication app = getSoundCloudApplication();
        final User user = new FetchUserTask(app).resolve(Request.to(Endpoints.MY_DETAILS));
        if (user == null) {
            // TODO: means we got a 404 on the user, needs to be more expressive...
            return new Result(new AuthorizationException(app.getContext().getString(R.string.authentication_error_no_connection_message)));
        }

        Log.d("LoginTask[User](" + user + ")");

        SignupVia signupVia = token.getSignup() != null ? SignupVia.fromString(token.getSignup()) : SignupVia.NONE;
        if (!addAccount(user, signupVia)) {
            // might mean the account already existed
            return new Result(new AuthorizationException(app.getContext().getString(R.string.authentication_error_no_connection_message)));
        }

        return new Result(user, signupVia);
    }
}
