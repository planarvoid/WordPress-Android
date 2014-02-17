package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.api.Token;

import android.os.Bundle;

import java.io.IOException;
//TODO Move into TokenOperations
public class TokenInformationGenerator {

    public static final String[] DEFAULT_SCOPES = {Token.SCOPE_NON_EXPIRING};
    private PublicCloudAPI mOldCloudAPI;

    public interface TokenKeys {
        String CODE_EXTRA = "code";
        String EXTENSION_GRANT_TYPE_EXTRA = "extensionGrantType";
        String USERNAME_EXTRA = "username";
        String PASSWORD_EXTRA = "password";
    }

    public TokenInformationGenerator(PublicCloudAPI oldCloudAPI){
        mOldCloudAPI = oldCloudAPI;
    }

    public Bundle getGrantBundle(String grantType, String token) {
        Bundle bundle = new Bundle();
        bundle.putString(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA, grantType + token);
        return bundle;
    }

    public Token getToken(Bundle param) throws IOException {
        if (param.containsKey(TokenKeys.CODE_EXTRA)) {
            return mOldCloudAPI.authorizationCode(param.getString(TokenKeys.CODE_EXTRA), DEFAULT_SCOPES);

        } else if (param.containsKey(TokenKeys.USERNAME_EXTRA)
                && param.containsKey(TokenKeys.PASSWORD_EXTRA)) {
            return mOldCloudAPI.login(param.getString(TokenKeys.USERNAME_EXTRA),
                    param.getString(TokenKeys.PASSWORD_EXTRA), DEFAULT_SCOPES);

        } else if (param.containsKey(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA)) {
            return mOldCloudAPI.extensionGrantType(param.getString(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA), DEFAULT_SCOPES);

        } else {
            throw new IllegalArgumentException("invalid param " + param);
        }
    }


}
