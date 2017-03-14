package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.crypto.Encryptor;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Config(sdk = 18)
public class SecureFileStorageTest extends AndroidUnitTest { // just because of logging

    @Mock private CryptoOperations operations;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private InputStream inputStream;
    @Mock private Encryptor.EncryptionProgressListener listener;

    private SecureFileStorage storage;
    private final Urn TRACK_URN = Urn.forTrack(123L);
    private final long MINIMUM_SPACE = 5L * 1024 * 1024;

    @Before
    public void setUp() throws Exception {
        when(settingsStorage.getOfflineContentLocation()).thenReturn(OfflineContentLocation.DEVICE_STORAGE);
        when(operations.generateHashForUrn(TRACK_URN)).thenReturn(TRACK_URN.toEncodedString());

        storage = new SecureFileStorage(operations, settingsStorage, context());
    }

    @Test
    public void offlineTracksDirectoryIsCreated() throws IOException, EncryptionException {
        storage.storeTrack(TRACK_URN, inputStream, listener);

        assertThat(storage.offlineDir.exists()).isTrue();
    }

    @Test
    public void offlineTrackDirectoryIsReusedWhenAlreadyExists() throws IOException, EncryptionException {
        final SecureFileStorage otherStorage = new SecureFileStorage(operations, settingsStorage, context());

        storage.storeTrack(TRACK_URN, inputStream, listener);
        otherStorage.storeTrack(Urn.forTrack(234L), inputStream, listener);

        assertThat(otherStorage.createDirectoryIfNeeded()).isTrue();
    }

    @Test
    public void storeTrackGeneratesFileNameFromTrackUrn() throws IOException, EncryptionException {
        storage.storeTrack(TRACK_URN, inputStream, listener);

        verify(operations).generateHashForUrn(TRACK_URN);
    }

    @Test
    public void storeTrackUsesCryptoOperationsToEncryptTheStream() throws IOException, EncryptionException {
        storage.storeTrack(TRACK_URN, inputStream, listener);

        verify(operations).encryptStream(eq(inputStream), any(OutputStream.class), same(listener));
    }

    @Test
    public void storeTrackSavesDataToAFile() throws Exception {
        final File file = new File(storage.offlineDir, TRACK_URN.toEncodedString() + ".enc");

        storage.storeTrack(TRACK_URN, inputStream, listener);

        assertThat(file.exists()).isTrue();
    }

    @Test(expected = EncryptionException.class)
    public void deletesFileWhenEncryptionFailed() throws IOException, EncryptionException {
        final File file = new File(storage.offlineDir, TRACK_URN.toEncodedString() + ".enc");

        final EncryptionException ioException = new EncryptionException("Test encrypt exception", new Exception());
        doThrow(ioException).when(operations).encryptStream(eq(inputStream), any(OutputStream.class), same(listener));

        storage.storeTrack(TRACK_URN, inputStream, listener);

        assertThat(file.exists()).isFalse();
    }

    @Test(expected = IOException.class)
    public void deletesFileWhenIOFailed() throws IOException, EncryptionException {
        final File file = new File(storage.offlineDir, TRACK_URN.toEncodedString() + ".enc");

        final IOException ioException = new IOException("Test IOException");
        doThrow(ioException).when(operations).encryptStream(eq(inputStream), any(OutputStream.class), same(listener));

        storage.storeTrack(TRACK_URN, inputStream, listener);

        assertThat(file.exists()).isFalse();
    }

    @Test
    public void returnsFileUriForTrack() throws Exception {
        assertThat(storage.getFileUriForOfflineTrack(TRACK_URN)).isEqualTo(Uri.fromFile(getEncryptedFile()));
    }

    @Test
    public void returnsEmptyUriWhenUnableToGenerateFileUri() throws Exception {
        when(operations.generateHashForUrn(TRACK_URN)).thenThrow(new EncryptionException("problems",
                                                                                         new IOException()));
        assertThat(storage.getFileUriForOfflineTrack(TRACK_URN)).isEqualTo(Uri.EMPTY);
    }

    @Test
    public void deleteTrackRemovesTrackFromStorage() throws Exception {
        final File file = createOfflineFile();
        storage.deleteTrack(TRACK_URN);
        assertThat(file.exists()).isFalse();
    }

    @Test
    public void shouldBeEnoughSpaceForTrackInStorageHappyCase() throws Exception {
        storage.offlineDir.mkdirs();
        when(settingsStorage.getStorageLimit()).thenReturn(1024L * 1024 * 1024);

        assertThat(storage.isEnoughSpace(1000)).isTrue();
    }

    @Test
    public void isEnoughSpaceShouldKeepSomeFreeSpaceOnDisk() throws Exception {
        storage.offlineDir.mkdirs();
        final long freeSpace = IOUtils.getExternalStorageDir(context()).getFreeSpace();
        when(settingsStorage.getStorageLimit()).thenReturn(freeSpace);

        assertThat(storage.isEnoughSpace(freeSpace)).isFalse();
    }

    @Test
    public void shouldNotBeEnoughSpaceForTrackInStorage() throws Exception {
        storage.offlineDir.mkdirs();
        when(settingsStorage.hasStorageLimit()).thenReturn(true);
        when(settingsStorage.getStorageLimit()).thenReturn(500L);

        assertThat(storage.isEnoughSpace(600L)).isFalse();
    }

    @Test
    public void isEnoughSpaceShouldReturnTrueUnderAbsoluteMaximumLimit() {
        storage.offlineDir.mkdirs();
        when(settingsStorage.hasStorageLimit()).thenReturn(true);
        when(settingsStorage.getStorageLimit()).thenReturn(500L);

        assertThat(storage.isEnoughSpace(500L)).isTrue();
    }

    @Test
    public void isEnoughSpaceShouldReturnFalseOverAbsoluteMaximumLimit() {
        storage.offlineDir.mkdirs();
        final long freeSpace = IOUtils.getExternalStorageDir(context()).getFreeSpace();
        when(settingsStorage.hasStorageLimit()).thenReturn(true);
        when(settingsStorage.getStorageLimit()).thenReturn(freeSpace);

        assertThat(storage.isEnoughSpace(freeSpace)).isFalse();
    }

    @Test
    public void deleteTrackRemovesAllTracksFromStorage() throws Exception {
        final File file = createOfflineFile();

        storage.deleteAllTracks();

        assertThat(file.exists()).isFalse();
        assertThat(storage.offlineDir.exists()).isTrue();
    }

    @Test
    public void returnsUsedStorage() throws Exception {
        assertThat(storage.getStorageUsed()).isEqualTo(0L);

        final File file = createOfflineFile();
        OutputStream os = new FileOutputStream(file);
        os.write(new byte[8192]);
        os.close();

        assertThat(storage.getStorageUsed()).isEqualTo(8192L);
        storage.deleteTrack(TRACK_URN);
    }

    @Test
    public void shouldBeEnoughMinimumSpaceWhenOnLimit() throws Exception {
        storage.offlineDir.mkdirs();
        when(settingsStorage.hasStorageLimit()).thenReturn(true);
        when(settingsStorage.getStorageLimit()).thenReturn(MINIMUM_SPACE);

        assertThat(storage.isEnoughMinimumSpace()).isTrue();
    }

    @Test
    public void shouldBeNotEnoughMinimumSpaceWhenBelowLimit() throws Exception {
        storage.offlineDir.mkdirs();
        when(settingsStorage.hasStorageLimit()).thenReturn(true);
        when(settingsStorage.getStorageLimit()).thenReturn(MINIMUM_SPACE - 1);

        assertThat(storage.isEnoughMinimumSpace()).isFalse();
    }

    private File createOfflineFile() throws IOException {
        final File file = new File(storage.offlineDir, TRACK_URN.toEncodedString() + ".enc");
        storage.offlineDir.mkdirs();
        file.createNewFile();
        assertThat(file.exists()).isTrue(); // just to ensure we have write permissions
        return file;
    }

    private File getEncryptedFile() {
        return new File(storage.offlineDir, TRACK_URN.toEncodedString() + ".enc");
    }
}
