package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;

import android.net.Uri;
import android.os.StatFs;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class SecureFileStorage {

    private static final String TAG = "SecureFileStorage";

    private static final String DIRECTORY_NAME = "offline";
    private static final String ENC_FILE_EXTENSION = ".enc";
    protected final File OFFLINE_DIR;

    private final CryptoOperations cryptoOperations;
    private final OfflineSettingsStorage settingsStorage;

    @Inject
    public SecureFileStorage(CryptoOperations operations, OfflineSettingsStorage settingsStorage) {
        this.cryptoOperations = operations;
        this.settingsStorage = settingsStorage;
        this.OFFLINE_DIR = new File(Consts.FILES_PATH, DIRECTORY_NAME);
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

    public boolean deleteTrack(Urn urn) throws EncryptionException {
        return new File(OFFLINE_DIR, generateFileName(urn)).delete();
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
        final long storageLimit = settingsStorage.getStorageLimit();
        final long trackSize = calculateFileSizeInBytes(trackDurationMillis);
        final long dirSizeWithTrack = getStorageUsed() + trackSize;
        return getStorageAvailable() > trackSize && storageLimit > dirSizeWithTrack;
    }

    @VisibleForTesting
    protected long calculateFileSizeInBytes(long trackDurationMillis) {
        //We assume 128 Kbps stereo MP3
        //File size in KB = (sec * bit) / 8 (Note kb is kilobytes, not kilobits, hence the 8).
        long trackSeconds = TimeUnit.MILLISECONDS.toSeconds(trackDurationMillis);
        long fileSizeKB = trackSeconds * 128 / 8L;
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
