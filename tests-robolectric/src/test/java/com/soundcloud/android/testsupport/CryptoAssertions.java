package com.soundcloud.android.testsupport;

import static com.soundcloud.android.Expect.expect;

public class CryptoAssertions {

    public static void expectByteArraysToBeEqual(byte[] expected, byte[] result) {
        expect(bytesToHex(expected)).toEqual(bytesToHex(result));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
            if (i % 16 == 15) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
