package com.soundcloud.android.activity.auth;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Token;

import android.os.Bundle;

import java.io.IOException;

public class TokenUtil {

    public static final String[] DEFAULT_SCOPES = {Token.SCOPE_NON_EXPIRING};

    public interface TokenKeys {
        String SCOPES_EXTRA = "scopes";
        String CODE_EXTRA = "code";
        String EXTENSION_GRANT_TYPE_EXTRA = "extensionGrantType";
        String USERNAME_EXTRA = "username";
        String PASSWORD_EXTRA = "password";
    }

    public static Bundle getGrantBundle(String grantType, String token) {
        Bundle bundle = new Bundle();
        bundle.putString(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA, grantType + token);
        return bundle;
    }

    public static Bundle configureDefaultScopeExtra(Bundle data) {
        if (!data.containsKey(TokenKeys.SCOPES_EXTRA)) {
            data.putStringArray(TokenKeys.SCOPES_EXTRA, DEFAULT_SCOPES);
        }
        return data;
    }

    public Token getToken(AndroidCloudAPI app, Bundle param) throws IOException {
        final String[] scopes = param.getStringArray(TokenKeys.SCOPES_EXTRA);

        if (param.containsKey(TokenKeys.CODE_EXTRA)) {
            return app.authorizationCode(param.getString(TokenUtil.TokenKeys.CODE_EXTRA), scopes);

        } else if (param.containsKey(TokenKeys.USERNAME_EXTRA)
                && param.containsKey(TokenKeys.PASSWORD_EXTRA)) {
            return app.login(param.getString(TokenKeys.USERNAME_EXTRA),
                    param.getString(TokenKeys.PASSWORD_EXTRA), scopes);

        } else if (param.containsKey(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA)) {
            return app.extensionGrantType(param.getString(TokenUtil.TokenKeys.EXTENSION_GRANT_TYPE_EXTRA), scopes);

        } else {
            throw new IllegalArgumentException("invalid param " + param);
        }
    }


}
