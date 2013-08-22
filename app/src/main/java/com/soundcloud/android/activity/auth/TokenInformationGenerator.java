package com.soundcloud.android.activity.auth;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Token;

import android.os.Bundle;

import java.io.IOException;
//TODO Move into TokenOperations
public class TokenInformationGenerator {

    public static final String[] DEFAULT_SCOPES = {Token.SCOPE_NON_EXPIRING};
    private AndroidCloudAPI mOldCloudAPI;

    public interface TokenKeys {
        String SCOPES_EXTRA = "scopes";
        String CODE_EXTRA = "code";
        String EXTENSION_GRANT_TYPE_EXTRA = "extensionGrantType";
        String USERNAME_EXTRA = "username";
        String PASSWORD_EXTRA = "password";
    }

    public TokenInformationGenerator(AndroidCloudAPI oldCloudAPI){
        mOldCloudAPI = oldCloudAPI;
    }

    public Bundle getGrantBundle(String grantType, String token) {
        Bundle bundle = new Bundle();
        bundle.putString(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA, grantType + token);
        return bundle;
    }

    public Bundle configureDefaultScopeExtra(Bundle data) {
        if (!data.containsKey(TokenKeys.SCOPES_EXTRA)) {
            data.putStringArray(TokenKeys.SCOPES_EXTRA, DEFAULT_SCOPES);
        }
        return data;
    }

    public Token getToken(Bundle param) throws IOException {
        final String[] scopes = param.getStringArray(TokenKeys.SCOPES_EXTRA);

        if (param.containsKey(TokenKeys.CODE_EXTRA)) {
            return mOldCloudAPI.authorizationCode(param.getString(TokenKeys.CODE_EXTRA), scopes);

        } else if (param.containsKey(TokenKeys.USERNAME_EXTRA)
                && param.containsKey(TokenKeys.PASSWORD_EXTRA)) {
            return mOldCloudAPI.login(param.getString(TokenKeys.USERNAME_EXTRA),
                    param.getString(TokenKeys.PASSWORD_EXTRA), scopes);

        } else if (param.containsKey(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA)) {
            return mOldCloudAPI.extensionGrantType(param.getString(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA), scopes);

        } else {
            throw new IllegalArgumentException("invalid param " + param);
        }
    }


}
