package com.soundcloud.android.crypto;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encryptor {

    private static final int BLOCK_SIZE = 8192;
    private static final String CIPHER_ALG = "AES/CBC/PKCS7Padding";
    private static final String KEY_ALG = "AES";
    private static final String HASH_ALG = "sha1";

    private Cipher cipher;

    @Inject
    public Encryptor() {
        /* no - op */
    }

    private void initCipher(DeviceSecret secret, int cipherMode) throws EncryptionException {
        try {
            final IvParameterSpec ivParam = new IvParameterSpec(secret.getInitVector());
            final SecretKey key = new SecretKeySpec(secret.getKey(), 0, secret.getKey().length, KEY_ALG);

            getCipher().init(cipherMode, key, ivParam);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Encryption algorithms not found", e);
        }
    }

    public void encrypt(InputStream in, OutputStream fos, DeviceSecret secret) throws EncryptionException, IOException {
        runCipher(in, fos, secret, Cipher.ENCRYPT_MODE);
    }

    public void decrypt(InputStream in, OutputStream fos, DeviceSecret secret) throws EncryptionException, IOException {
        runCipher(in, fos, secret, Cipher.DECRYPT_MODE);
    }

    private Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        if (cipher == null) {
            cipher = Cipher.getInstance(CIPHER_ALG);
        }
        return cipher;
    }

    private void runCipher(InputStream input, OutputStream output, DeviceSecret secret, int cipherMode) throws EncryptionException, IOException {
        try {
            initCipher(secret, cipherMode);

            int readBytes;
            int cipherBytes;
            byte[] buffer = new byte[BLOCK_SIZE];
            byte[] encrypted = new byte[cipher.getOutputSize(buffer.length)];

            while ((readBytes = input.read(buffer)) != -1) {
                cipherBytes = cipher.update(buffer, 0, readBytes, encrypted);
                output.write(encrypted, 0, cipherBytes);
            }

            cipherBytes = cipher.doFinal(encrypted, 0);
            output.write(encrypted, 0, cipherBytes);

        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Failed to encrypt a file", e);
        }
    }

    @VisibleForTesting
    protected String hash(Urn trackUrn, MessageDigest digest) {
        final byte[] bytes = trackUrn.toEncodedString().getBytes(Charsets.UTF_8);
        return ScTextUtils.hexString(digest.digest(bytes));
    }

    public String hash(Urn trackUrn) throws EncryptionException {
        try {
            final MessageDigest digest = MessageDigest.getInstance(HASH_ALG);
            return hash(trackUrn, digest);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException("No provider found for sha1 digest", e);
        }
    }

}
