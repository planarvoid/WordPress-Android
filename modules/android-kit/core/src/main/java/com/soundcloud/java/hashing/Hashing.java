package com.soundcloud.java.hashing;

import com.soundcloud.java.strings.Charsets;
import com.soundcloud.java.strings.Strings;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hashing {

    /**
     * Hashes the given input string using MD5 and returns it as a hex string.
     */
    public static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(input.getBytes(Charsets.UTF_8));
            return Strings.toHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Hashing() {
        // no instances
    }
}
