package com.soundcloud.android.crypto;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class CipherWrapper {

    private Cipher cipher;

    @Inject
    public CipherWrapper() {
        /* no -op */
    }

    public int getOutputSize(int length) {
        return cipher.getOutputSize(length);
    }

    public int update(byte[] buffer, int i, int readBytes, byte[] encrypted) throws EncryptionException {
        try {
            return cipher.update(buffer, i, readBytes, encrypted);
        } catch (ShortBufferException e) {
            throw new EncryptionException("Failed to call cipher.update", e);
        }
    }

    public int doFinal(byte[] output, int outputOffset) throws EncryptionException {
        try {
            return cipher.doFinal(output, outputOffset);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Failed to call finish encryption", e);
        }
    }

    public void init(String cipherAlgorithm, int cipherMode, DeviceSecret secret, String keyAlgorithm)
            throws EncryptionException {
        try {
            final IvParameterSpec ivParam = new IvParameterSpec(secret.getInitVector());
            final SecretKey key = new SecretKeySpec(secret.getKey(), 0, secret.getKey().length, keyAlgorithm);
            getCipher(cipherAlgorithm).init(cipherMode, key, ivParam);

        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new EncryptionException("Failed to get cipher instance", e);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new EncryptionException("Failed to init cipher with given key and iv", e);
        }
    }

    private Cipher getCipher(String cipherAlgorithm) throws NoSuchPaddingException, NoSuchAlgorithmException {
        if (cipher == null) {
            cipher = Cipher.getInstance(cipherAlgorithm);
        }
        return cipher;
    }

}
