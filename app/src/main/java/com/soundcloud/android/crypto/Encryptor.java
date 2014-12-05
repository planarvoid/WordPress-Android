package com.soundcloud.android.crypto;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
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

    private static final int BLOCK_SIZE = 32768;
    private static final int HMAC_LENGTH = 16;
    private static final String CIPHER_ALG = "AES/CBC/PKCS7Padding";
    private static final String HMAC_ALG = "HmacMD5";
    private static final String KEY_ALG = "AES";
    private static final String HASH_ALG = "sha1";

    private Mac mac;
    private Cipher cipher;

    @Inject
    public Encryptor() {
        /* no - op */
    }

    private void initEncryption(DeviceSecret secret) throws EncryptionException {
        try {
            final IvParameterSpec ivParam = new IvParameterSpec(secret.getInitVector());
            final SecretKey key = new SecretKeySpec(secret.getKey(), 0, secret.getKey().length, KEY_ALG);

            if (cipher == null || mac == null) {
                cipher = Cipher.getInstance(CIPHER_ALG);
                mac = Mac.getInstance(HMAC_ALG);
            }

            cipher.init(Cipher.ENCRYPT_MODE, key, ivParam);
            mac.init(key);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Encryption algorithms not found", e);
        }
    }

    public void encryptFile(InputStream in, OutputStream fos, DeviceSecret secret) throws EncryptionException, IOException {
        try {
            initEncryption(secret);
            byte[] block = new byte[BLOCK_SIZE - HMAC_LENGTH];

            int readBytes;
            while ((readBytes = in.read(block)) != -1) {

                byte[] encrypted;
                if (readBytes < block.length) {
                    byte[] slice = new byte[readBytes];
                    System.arraycopy(block, 0, slice, 0, readBytes);
                    encrypted = cipher.doFinal(slice);
                } else {
                    encrypted = cipher.update(block);
                }

                byte[] hmac = mac.doFinal(encrypted);
                fos.write(encrypted);
                fos.write(hmac);
            }
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
