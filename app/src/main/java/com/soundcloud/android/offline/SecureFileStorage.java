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
import javax.inject.Singleton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Singleton
public class SecureFileStorage {

    private static final String TAG = "SecureFileStorage";
    private static final String DIRECTORY_NAME = "offline";
    private static final String ENC_FILE_EXTENSION = ".enc";
    private static final int MINIMUM_SPACE = 5 * 1024 * 1024; // 5MB

    @Nullable File offlineDir;

    private final CryptoOperations cryptoOperations;
    private final OfflineSettingsStorage settingsStorage;
    private final Context context;
    private volatile boolean isRunningEncryption;

    @Inject
    public SecureFileStorage(CryptoOperations operations, OfflineSettingsStorage settingsStorage, Context context) {
        this.cryptoOperations = operations;
        this.settingsStorage = settingsStorage;
        this.context = context;
        updateOfflineDir();
    }

    void updateOfflineDir() {
        offlineDir = OfflineContentLocation.DEVICE_STORAGE == settingsStorage.getOfflineContentLocation()
                     ? IOUtils.createExternalStorageDir(context, DIRECTORY_NAME)
                     : IOUtils.createSDCardDir(context, DIRECTORY_NAME);
    }

    void tryCancelRunningEncryption() {
        if (isRunningEncryption) {
            cryptoOperations.cancelEncryption();
        }
    }

    void storeTrack(Urn urn,
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

    boolean isEnoughMinimumSpace() {
        return isEnoughSpace(MINIMUM_SPACE);
    }

    private boolean deleteFile(File file) {
        return !file.exists() || file.delete();
    }

    boolean deleteTrack(Urn urn) {
        try {
            return offlineDir != null && deleteFile(new File(offlineDir, generateFileName(urn)));
        } catch (EncryptionException exception) {
            ErrorUtils.handleSilentException("Offline file deletion failed for track " + urn, exception);
            return false;
        }
    }

    void deleteAllTracks() {
        if (offlineDir != null) {
            IOUtils.cleanDir(offlineDir);
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

    boolean isEnoughSpace(long sizeInBytes) {
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
        return IOUtils.getStorageFreeSpace(offlineDir);
    }

    public long getStorageCapacity() {
        return IOUtils.getStorageTotalSpace(offlineDir);
    }

    private String generateFileName(Urn urn) throws EncryptionException {
        return cryptoOperations.generateHashForUrn(urn) + ENC_FILE_EXTENSION;
    }

    @VisibleForTesting
    final boolean createDirectoryIfNeeded() {
        return offlineDir != null && (offlineDir.exists() || IOUtils.mkdirs(offlineDir));
    }

}
