package com.soundcloud.android.gcm;

import com.soundcloud.android.crypto.CipherWrapper;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.java.strings.Charsets;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import java.io.UnsupportedEncodingException;

class GcmDecryptor {

    private static final String INITIALIZATION_VECTOR = "1111111111ABCDEF1111111111ABCDEF";
    private static final String KEY_DATA = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";
    private static final byte[] key = hexStringToByteArray(new String(Base64.decode(KEY_DATA, Base64.DEFAULT)));
    private static final byte[] iv = hexStringToByteArray(INITIALIZATION_VECTOR);

    private final CipherWrapper cipherWrapper;

    final IvParameterSpec ivParam = new IvParameterSpec(iv);
    final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

    @Inject
    public GcmDecryptor(CipherWrapper cipherWrapper) {
        this.cipherWrapper = cipherWrapper;
    }

    public String decrypt(String payload) throws EncryptionException, UnsupportedEncodingException {
        cipherWrapper.init("AES/CBC/PKCS7Padding", Cipher.DECRYPT_MODE, ivParam, skeySpec);
        final byte[] decryptedBytes = cipherWrapper.finish(Base64.decode(payload, 0));
        return new String(decryptedBytes, Charsets.UTF_8.name());
    }

    // utility function, not really useful outside this class
    private static byte[] hexStringToByteArray(String string) {
        int len = string.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                    + Character.digit(string.charAt(i+1), 16));
        }
        return data;
    }

}


