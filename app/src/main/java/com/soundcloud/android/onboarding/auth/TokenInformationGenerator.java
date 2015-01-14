package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;

import android.os.Bundle;

import java.io.IOException;
//TODO Move into TokenOperations
public class TokenInformationGenerator {

    private final PublicCloudAPI oldCloudAPI;

    public interface TokenKeys {
        String EXTENSION_GRANT_TYPE_EXTRA = "extensionGrantType";
        String USERNAME_EXTRA = "username";
        String PASSWORD_EXTRA = "password";
    }

    public TokenInformationGenerator(PublicCloudAPI oldCloudAPI){
        this.oldCloudAPI = oldCloudAPI;
    }

    public Bundle getGrantBundle(String grantType, String token) {
        Bundle bundle = new Bundle();
        bundle.putString(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA, grantType + token);
        return bundle;
    }

    public Token getToken(Bundle param) throws IOException {
        if (param.containsKey(TokenKeys.USERNAME_EXTRA) && param.containsKey(TokenKeys.PASSWORD_EXTRA)) {
            // User entered username and password
            return oldCloudAPI.login(param.getString(TokenKeys.USERNAME_EXTRA),
                    param.getString(TokenKeys.PASSWORD_EXTRA));

        } else if (param.containsKey(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA)) {
            // User logged in with Google Plus or Facebook
            return oldCloudAPI.extensionGrantType(extractGrantType(param));

        } else {
            throw new IllegalArgumentException("invalid param " + param);
        }
    }

    public boolean isFromFacebook(Bundle data) {
        final String grantType = extractGrantType(data);
        return grantType != null && grantType.startsWith(OAuth.GRANT_TYPE_FACEBOOK);
    }

    private String extractGrantType(Bundle data) {
        return data.getString(TokenKeys.EXTENSION_GRANT_TYPE_EXTRA);
    }

}
