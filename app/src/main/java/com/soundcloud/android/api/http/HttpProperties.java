package com.soundcloud.android.api.http;


import com.soundcloud.android.R;

import android.content.res.Resources;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class HttpProperties {

    private final long[] PRODUCTION =
            new long[]{0xCFDBF8AB10DCADA3L, 0x6C580A13A4B7801L, 0x607547EC749EBFB4L,
                    0x300C455E649B39A7L, 0x20A6BAC9576286CBL};

    private String clientId;

    public HttpProperties(Resources context) {
        clientId = context.getString(R.string.client_id);
    }

    public String getClientSecret() {
        return deobfuscate(PRODUCTION);
    }


    public String getClientId(){
        return clientId;
    }
    /**
     * Based on
     * <a href="http://truelicense.java.net/apidocs/de/schlichtherle/util/ObfuscatedString.html">
     *  ObfuscatedString
     * </a>
     * @param obfuscated the obfuscated array
     * @return unobfuscated string
     */
    private String deobfuscate(long[] obfuscated) {
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
