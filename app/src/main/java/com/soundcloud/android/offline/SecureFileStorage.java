package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;

import android.net.Uri;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class SecureFileStorage {

    public static final int MP3_128_KBPS = 128;

    private static final String TAG = "SecureFileStorage";
    private static final String DIRECTORY_NAME = "offline";
    private static final String ENC_FILE_EXTENSION = ".enc";

    protected final File OFFLINE_DIR;

    private final CryptoOperations cryptoOperations;
    private final OfflineSettingsStorage settingsStorage;
    private volatile boolean isRunningEncryption;

    @Inject
    public SecureFileStorage(CryptoOperations operations, OfflineSettingsStorage settingsStorage) {
        this.cryptoOperations = operations;
        this.settingsStorage = settingsStorage;
        this.OFFLINE_DIR = new File(Consts.FILES_PATH, DIRECTORY_NAME);
    }

    public void tryCancelRunningEncryption() {
        if (isRunningEncryption) {
            cryptoOperations.cancelEncryption();
        }
    }

    public void storeTrack(Urn urn, InputStream input) throws IOException, EncryptionException {
        if (!createDirectoryIfNeeded()) {
            throw new IOException("Failed to create directory for " + OFFLINE_DIR.getAbsolutePath());
        }

        final File trackFile = new File(OFFLINE_DIR, generateFileName(urn));
        final OutputStream output = new FileOutputStream(trackFile);
        isRunningEncryption = true;

        try {
            cryptoOperations.encryptStream(input, output);
        } catch (Exception e) {
            IOUtils.close(output);
            deleteFile(trackFile);

            throw e;
        } finally {
            isRunningEncryption = false;
            IOUtils.close(output);
        }
    }

    private boolean deleteFile(File file) {
        return !file.exists() || file.delete();
    }

    public boolean deleteTrack(Urn urn) {
        try {
            return deleteFile(new File(OFFLINE_DIR, generateFileName(urn)));
        } catch (EncryptionException exception) {
            ErrorUtils.handleSilentException("Offline file deletion failed for track " + urn, exception);
            return false;
        }
    }

    public void deleteAllTracks() {
        IOUtils.deleteDir(OFFLINE_DIR);
    }

    public Uri getFileUriForOfflineTrack(Urn urn) {
        try {
            return Uri.fromFile(new File(OFFLINE_DIR, generateFileName(urn)));
        } catch (EncryptionException e) {
            Log.e(TAG, "Unable to generate file uri ", e);
        }
        return Uri.EMPTY;
    }

    public boolean isEnoughSpaceForTrack(long trackDurationMillis) {
        long trackSize = calculateFileSizeInBytes(trackDurationMillis);
        long dirSizeWithTrack = getStorageUsed() + trackSize;
        return getStorageAvailable() > trackSize && isWithinStorageLimit(dirSizeWithTrack);
    }

    private boolean isWithinStorageLimit(long dirSizeWithTrack) {
        return !settingsStorage.hasStorageLimit() || settingsStorage.getStorageLimit() > dirSizeWithTrack;
    }

    @VisibleForTesting
    protected long calculateFileSizeInBytes(long trackDurationMillis) {
        long trackSeconds = TimeUnit.MILLISECONDS.toSeconds(trackDurationMillis);
        long fileSizeKB = trackSeconds * MP3_128_KBPS / 8L; //(KB is kilobytes, not kilobits, hence the 8).
        return fileSizeKB * 1024;
    }

    public long getStorageUsed() {
        return IOUtils.getDirSize(OFFLINE_DIR);
    }

    public long getStorageAvailable() {
        return Consts.FILES_PATH.getFreeSpace();
    }

    public long getStorageCapacity() {
        return Consts.FILES_PATH.getTotalSpace();
    }

    private String generateFileName(Urn urn) throws EncryptionException {
        return cryptoOperations.generateHashForUrn(urn) + ENC_FILE_EXTENSION;
    }

    @VisibleForTesting
    protected final boolean createDirectoryIfNeeded() {
        return OFFLINE_DIR.exists() || IOUtils.mkdirs(OFFLINE_DIR);
    }
}
