package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RunWith(SoundCloudTestRunner.class)
public class SecureFileStorageTest {

    @Mock private CryptoOperations operations;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private InputStream inputStream;

    private SecureFileStorage storage;
    private final Urn TRACK_URN = Urn.forTrack(123L);

    @Before
    public void setUp() throws Exception {
        storage = new SecureFileStorage(operations, settingsStorage);
        when(operations.generateHashForUrn(TRACK_URN)).thenReturn(TRACK_URN.toEncodedString());
    }

    @Test
    public void offlineTracksDirectoryIsCreated() throws IOException, EncryptionException {
        storage.storeTrack(TRACK_URN, inputStream);

        expect(storage.OFFLINE_DIR.exists()).toBeTrue();
    }

    @Test
    public void offlineTrackDirectoryIsReusedWhenAlreadyExists() throws IOException, EncryptionException {
        final SecureFileStorage otherStorage = new SecureFileStorage(operations, settingsStorage);

        storage.storeTrack(TRACK_URN, inputStream);
        otherStorage.storeTrack(Urn.forTrack(234L), inputStream);

        expect(otherStorage.createDirectoryIfNeeded()).toBeTrue();
    }

    @Test
    public void storeTrackGeneratesFileNameFromTrackUrn() throws IOException, EncryptionException {
        storage.storeTrack(TRACK_URN, inputStream);

        verify(operations).generateHashForUrn(TRACK_URN);
    }

    @Test
    public void storeTrackUsesCryptoOperationsToEncryptTheStream() throws IOException, EncryptionException {
        storage.storeTrack(TRACK_URN, inputStream);

        verify(operations).encryptStream(eq(inputStream), any(OutputStream.class));
    }

    @Test
    public void storeTrackSavesDataToAFile() throws Exception {
        final File file = new File(storage.OFFLINE_DIR, TRACK_URN.toEncodedString() + ".enc");

        storage.storeTrack(TRACK_URN, inputStream);

        expect(file.exists()).toBeTrue();
    }

    @Test(expected = EncryptionException.class)
    public void deletesFileWhenEncryptionFailed() throws IOException, EncryptionException {
        final File file = new File(storage.OFFLINE_DIR, TRACK_URN.toEncodedString() + ".enc");

        final EncryptionException ioException = new EncryptionException("Test encrypt exception", new Exception());
        doThrow(ioException).when(operations).encryptStream(eq(inputStream), any(OutputStream.class));

        storage.storeTrack(TRACK_URN, inputStream);

        expect(file.exists()).toBeFalse();
    }

    @Test(expected = IOException.class)
    public void deletesFileWhenIOFailed() throws IOException, EncryptionException {
        final File file = new File(storage.OFFLINE_DIR, TRACK_URN.toEncodedString() + ".enc");

        final IOException ioException = new IOException("Test IOException");
        doThrow(ioException).when(operations).encryptStream(eq(inputStream), any(OutputStream.class));

        storage.storeTrack(TRACK_URN, inputStream);

        expect(file.exists()).toBeFalse();
    }

    @Test
    public void returnsFileUriForTrack() throws Exception {
        expect(storage.getFileUriForOfflineTrack(TRACK_URN)).toEqual(Uri.fromFile(getEncryptedFile()));
    }

    @Test
    public void returnsEmptyUriWhenUnableToGenerateFileUri() throws Exception {
        when(operations.generateHashForUrn(TRACK_URN)).thenThrow(new EncryptionException("problems", new IOException()));
        expect(storage.getFileUriForOfflineTrack(TRACK_URN)).toEqual(Uri.EMPTY);
    }

    @Test
    public void deleteTrackRemovesTrackFromStorage() throws Exception {
        final File file = createOfflineFile();
        storage.deleteTrack(TRACK_URN);
        expect(file.exists()).toBeFalse();
    }

    @Test
    public void shouldBeEnoughSpaceForTrackInStorageHappyCase() throws Exception {
        storage.OFFLINE_DIR.mkdirs();
        when(settingsStorage.getStorageLimit()).thenReturn(1024L * 1024 * 1024);

        expect(storage.isEnoughSpaceForTrack(1000)).toBeTrue();
    }

    @Test
    public void shouldNotBeEnoughSpaceForTrackInStorage() throws Exception {
        storage.OFFLINE_DIR.mkdirs();
        when(settingsStorage.hasStorageLimit()).thenReturn(true);
        when(settingsStorage.getStorageLimit()).thenReturn(500L);

        expect(storage.isEnoughSpaceForTrack(1000000L)).toBeFalse();
    }

    @Test
    public void calculateCorrectFileSizeBasedOnTrackDurationMp3WithBitRate128Stereo() {
        long fileSizeFor1SecondTrackDuration = storage.calculateFileSizeInBytes(1000);
        long fileSizeFor1MinuteTrackDuration = storage.calculateFileSizeInBytes(1000 * 60);

        expect(fileSizeFor1SecondTrackDuration).toEqual(16384L);   //16 KB
        expect(fileSizeFor1MinuteTrackDuration).toEqual(983040L);  //960 KB
    }

    @Test
    public void deleteTrackRemovesAllTracksFromStorage() throws Exception {
        final File file = createOfflineFile();

        storage.deleteAllTracks();

        expect(file.exists()).toBeFalse();
        expect(storage.OFFLINE_DIR.exists()).toBeFalse();
    }

    @Test
    public void returnsUsedStorage() throws Exception {
        expect(storage.getStorageUsed()).toEqual(0l);

        final File file = createOfflineFile();
        OutputStream os = new FileOutputStream(file);
        os.write(new byte[8192]);
        os.close();

        expect(storage.getStorageUsed()).toEqual(8192l);
        storage.deleteTrack(TRACK_URN);
    }

    private File createOfflineFile() throws IOException {
        final File file = new File(storage.OFFLINE_DIR, TRACK_URN.toEncodedString() + ".enc");
        storage.OFFLINE_DIR.mkdirs();
        file.createNewFile();
        expect(file.exists()).toBeTrue(); // just to ensure we have write permissions
        return file;
    }

    private File getEncryptedFile() {
        return new File(storage.OFFLINE_DIR, TRACK_URN.toEncodedString() + ".enc");
    }
}