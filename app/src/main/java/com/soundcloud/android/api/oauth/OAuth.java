package com.soundcloud.android.api.oauth;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.crypto.Obfuscator;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

public class OAuth {
    private static final String CLIENT_ID = "dbdsA8b6V6Lw7wzu1x0T4CLxt58yd4Bf";
    private static final String OBFUSCATED_CLIENT_SECRET = "NykCWyEEEyUrRCd2AQAtEAUdfy9HKAAkKRwjJh4cMSk=";

    private final String clientId;
    private final String clientSecret;
    private final AccountOperations accountOperations;

    @Inject
    public OAuth(AccountOperations accountOperations, Obfuscator obfuscator) {
        this(CLIENT_ID, obfuscator.deobfuscateString(OBFUSCATED_CLIENT_SECRET), accountOperations);
    }

    @VisibleForTesting
    public OAuth(String clientId, String clientSecret, AccountOperations accountOperations) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accountOperations = accountOperations;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public static String createOAuthHeaderValue(Token token) {
        boolean valid = token != null && token.valid();
        String tokenString = valid ? token.getAccessToken() : "invalidated";
        return "OAuth " + tokenString;
    }

    public String getAuthorizationHeaderValue() {
        return createOAuthHeaderValue(accountOperations.getSoundCloudToken());
    }
}
