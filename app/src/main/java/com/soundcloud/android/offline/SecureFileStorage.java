package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.IOUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SecureFileStorage {

    private static final String DIRECTORY_NAME = "offline";

    private final CryptoOperations cryptoOperations;
    protected final File OFFLINE_DIR;

    @Inject
    public SecureFileStorage(CryptoOperations operations) {
        this.cryptoOperations = operations;
        this.OFFLINE_DIR = new File(Consts.FILES_PATH, DIRECTORY_NAME);
        createDirectoryIfNeeded();
    }

    public void storeTrack(Urn urn, InputStream input) throws IOException, EncryptionException {
        if (!createDirectoryIfNeeded()) {
            throw new IOException("Failed to create directory for " + OFFLINE_DIR.getAbsolutePath());
        }

        final File trackFile = new File(OFFLINE_DIR, generateFileName(urn));
        final OutputStream output = new FileOutputStream(trackFile);
        try {
            cryptoOperations.encryptStream(input, output);
        } finally {
            IOUtils.close(output);
        }
    }

    private String generateFileName(Urn urn) throws EncryptionException {
        return cryptoOperations.generateHashForUrn(urn);
    }

    @VisibleForTesting
    protected boolean createDirectoryIfNeeded() {
        return OFFLINE_DIR.exists() || IOUtils.mkdirs(OFFLINE_DIR);
    }
}
