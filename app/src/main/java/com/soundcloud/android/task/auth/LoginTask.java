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

public class LoginTask extends AuthTask {
    public static final String[] SCOPES_TO_REQUEST = { Token.SCOPE_NON_EXPIRING };
    public static final String SCOPES_EXTRA = "scopes";
    private final SoundCloudApplication mApp;


    public LoginTask(@NotNull SoundCloudApplication application) {
        mApp = application;
    }

    public SoundCloudApplication getSoundCloudApplication(){
        return mApp;
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

        final GetTokensTask getTokensTask = new GetTokensTask(mApp);
        Token token = getTokensTask.getToken(data);
        if (token == null) { // no tokens obtained
            return new Result(getTokensTask.getException());
        }
        Log.d("LoginTask[Token](" + token + ")");

        final User user = new FetchUserTask(mApp).resolve(Request.to(Endpoints.MY_DETAILS));
        if (user == null) {
            // TODO: means we got a 404 on the user, needs to be more expressive...
            return new Result(new UnableToCreateAccountException());
        }

        Log.d("LoginTask[User](" + user + ")");

        SignupVia signupVia = token.getSignup() != null ? SignupVia.fromString(token.getSignup()) : SignupVia.NONE;
        if (!addAccount(mApp, user, signupVia)) {
            // might mean the account already existed
            return new Result(new UnableToCreateAccountException());
        }

        return new Result(user, signupVia);
    }




}
