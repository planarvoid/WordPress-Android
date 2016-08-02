package com.soundcloud.android.offline;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.crypto.Encryptor;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SecureFileStorage {

    private static final String TAG = "SecureFileStorage";
    private static final String DIRECTORY_NAME = "offline";
    private static final String ENC_FILE_EXTENSION = ".enc";
    private static final int FREE_SPACE_BUFFER = 100 * 1024 * 1024;
    private static final int MINIMUM_SPACE = 5 * 1024 * 1024; // 5MB

    @Nullable
    protected final File offlineDir;

    private final CryptoOperations cryptoOperations;
    private final OfflineSettingsStorage settingsStorage;
    private volatile boolean isRunningEncryption;

    @Inject
    public SecureFileStorage(CryptoOperations operations, OfflineSettingsStorage settingsStorage, Context context) {
        this.cryptoOperations = operations;
        this.settingsStorage = settingsStorage;
        this.offlineDir = IOUtils.getExternalStorageDir(context, DIRECTORY_NAME);
    }

    public void tryCancelRunningEncryption() {
        if (isRunningEncryption) {
            cryptoOperations.cancelEncryption();
        }
    }

    public void storeTrack(Urn urn,
                           InputStream input,
                           Encryptor.EncryptionProgressListener listener) throws IOException, EncryptionException {
        if (!createDirectoryIfNeeded()) {
            throw new IOException("Failed to create directory for " + offlineDir);
        }

        final File trackFile = new File(offlineDir, generateFileName(urn));
        final OutputStream output = new FileOutputStream(trackFile);
        isRunningEncryption = true;

        try {
            cryptoOperations.encryptStream(input, output, listener);
        } catch (Exception e) {
            IOUtils.close(output);
            deleteFile(trackFile);

            throw e;
        } finally {
            isRunningEncryption = false;
            IOUtils.close(output);
        }
    }

    public boolean isEnoughMinimumSpace() {
        return isEnoughSpace(MINIMUM_SPACE);
    }

    private boolean deleteFile(File file) {
        return !file.exists() || file.delete();
    }

    public boolean deleteTrack(Urn urn) {
        try {
            return offlineDir != null && deleteFile(new File(offlineDir, generateFileName(urn)));
        } catch (EncryptionException exception) {
            ErrorUtils.handleSilentException("Offline file deletion failed for track " + urn, exception);
            return false;
        }
    }

    public void deleteAllTracks() {
        if (offlineDir != null) {
            IOUtils.deleteDir(offlineDir);
        }
    }

    public Uri getFileUriForOfflineTrack(Urn urn) {
        if (offlineDir != null) {
            try {
                return Uri.fromFile(new File(offlineDir, generateFileName(urn)));
            } catch (EncryptionException e) {
                Log.e(TAG, "Unable to generate file uri ", e);
            }
        }
        return Uri.EMPTY;
    }

    public boolean isEnoughSpace(long sizeInBytes) {
        long dirSizeWithFile = getStorageUsed() + sizeInBytes;
        final long storageAvailable = getStorageAvailable();
        return storageAvailable > 0 && storageAvailable >= sizeInBytes && isWithinStorageLimit(dirSizeWithFile);
    }

    private boolean isWithinStorageLimit(long dirSizeWithTrack) {
        return !settingsStorage.hasStorageLimit() || settingsStorage.getStorageLimit() >= dirSizeWithTrack;
    }

    public long getStorageUsed() {
        return offlineDir == null ? 0 : IOUtils.getDirSize(offlineDir);
    }

    public long getStorageAvailable() {
        return offlineDir == null ? 0 : Math.max(offlineDir.getFreeSpace() - FREE_SPACE_BUFFER, 0);
    }

    public long getStorageCapacity() {
        return offlineDir == null ? 0 : offlineDir.getTotalSpace();
    }

    private String generateFileName(Urn urn) throws EncryptionException {
        return cryptoOperations.generateHashForUrn(urn) + ENC_FILE_EXTENSION;
    }

    @VisibleForTesting
    protected final boolean createDirectoryIfNeeded() {
        return offlineDir != null && (offlineDir.exists() || IOUtils.mkdirs(offlineDir));
    }

}
