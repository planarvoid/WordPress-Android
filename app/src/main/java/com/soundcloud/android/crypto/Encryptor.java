package com.soundcloud.android.crypto;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;

import android.support.annotation.Nullable;

import javax.crypto.Cipher;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Encryptor {

    private static final int REPORT_INTERVAL = 20; // prevents spammy progress reporting
    private static final int BLOCK_SIZE = 8192;
    private static final String CIPHER_ALG = "AES/CBC/PKCS7Padding";
    private static final String KEY_ALG = "AES";
    private static final String HASH_ALG = "sha1";

    private final CipherWrapper cipher;
    private final AtomicBoolean cancelRequest = new AtomicBoolean();

    @Inject
    public Encryptor(CipherWrapper cipher) {
        this.cipher = cipher;
    }

    public void encrypt(InputStream in, OutputStream out, DeviceSecret secret, EncryptionProgressListener listener) throws EncryptionException, IOException {
        runCipher(in, out, secret, Cipher.ENCRYPT_MODE, listener);
    }

    public void decrypt(InputStream in, OutputStream out, DeviceSecret secret) throws EncryptionException, IOException {
        runCipher(in, out, secret, Cipher.DECRYPT_MODE, null);
    }

    public void tryToCancelRequest() {
        cancelRequest.set(true);
    }

    private void runCipher(InputStream in, OutputStream out, DeviceSecret secret, int cipherMode, @Nullable EncryptionProgressListener listener)
            throws EncryptionException, IOException {

        initCipher(secret, cipherMode);

        int readBytes;
        int cipherBytes;
        byte[] buffer = new byte[BLOCK_SIZE];
        byte[] encrypted = new byte[cipher.getOutputSize(buffer.length)];

        long processed = 0;

        while (!cancelRequest.get() && (readBytes = in.read(buffer)) != -1) {
            cipherBytes = cipher.update(buffer, 0, readBytes, encrypted);
            out.write(encrypted, 0, cipherBytes);
            processed += readBytes;

            if (listener != null && (processed / buffer.length) % REPORT_INTERVAL == 0){
                listener.onBytesEncrypted(processed);
            }
        }

        if (cancelRequest.getAndSet(false)) {
            throw new EncryptionInterruptedException("File encryption cancelled");
        } else {
            if (listener != null) {
                // send final report
                listener.onBytesEncrypted(processed);
            }
        }

        cipherBytes = cipher.doFinal(encrypted, 0);
        out.write(encrypted, 0, cipherBytes);
    }

    private void initCipher(DeviceSecret secret, int cipherMode) throws EncryptionException {
        cipher.init(CIPHER_ALG, cipherMode, secret, KEY_ALG);
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

    public interface EncryptionProgressListener {
        void onBytesEncrypted(long totalProcessed);
    }
}
