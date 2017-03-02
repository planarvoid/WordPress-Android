package com.soundcloud.android.api.oauth;

import com.soundcloud.android.accounts.AccountOperations;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.Random;

public class OAuth {
    private static final String CLIENT_ID = "40ccfee680a844780a41fbe23ea89934";
    private static final long[] CLIENT_SECRET =
            new long[]{0xCFDBF8AB10DCADA3L, 0x6C580A13A4B7801L, 0x607547EC749EBFB4L,
                    0x300C455E649B39A7L, 0x20A6BAC9576286CBL};

    private final String clientId;
    private final String clientSecret;
    private final AccountOperations accountOperations;

    @Inject
    public OAuth(AccountOperations accountOperations) {
        this(CLIENT_ID, deobfuscate(CLIENT_SECRET), accountOperations);
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

    /**
     * Based on
     * <a href="http://truelicense.java.net/apidocs/de/schlichtherle/util/ObfuscatedString.html">
     * ObfuscatedString
     * </a>
     *
     * @param obfuscated the obfuscated array
     * @return unobfuscated string
     */
    private static String deobfuscate(long[] obfuscated) {
        final int length = obfuscated.length;
        // The original UTF8 encoded string was probably not a multiple
        // of eight bytes long and is thus actually shorter than this array.
        final byte[] encoded = new byte[8 * (length - 1)];
        // Obtain the seed and initialize a new PRNG with it.
        final long seed = obfuscated[0];
        final Random prng = new Random(seed);

        // De-obfuscate.
        for (int i = 1; i < length; i++) {
            final long key = prng.nextLong();
            long l = obfuscated[i] ^ key;
            final int end = Math.min(encoded.length, 8 * (i - 1) + 8);
            for (int i1 = 8 * (i - 1); i1 < end; i1++) {
                encoded[i1] = (byte) l;
                l >>= 8;
            }
        }

        // Decode the UTF-8 encoded byte array into a string.
        // This will create null characters at the end of the decoded string
        // in case the original UTF8 encoded string was not a multiple of
        // eight bytes long.
        final String decoded;
        try {
            decoded = new String(encoded,
                                 new String(new char[]{'\u0055', '\u0054', '\u0046', '\u0038'}) /* UTF8 */);
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex); // UTF-8 is always supported
        }

        // Cut off trailing null characters in case the original UTF8 encoded
        // string was not a multiple of eight bytes long.
        final int i = decoded.indexOf(0);
        return -1 == i ? decoded : decoded.substring(0, i);
    }
}
