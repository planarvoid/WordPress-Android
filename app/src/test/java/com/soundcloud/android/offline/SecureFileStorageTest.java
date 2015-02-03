package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RunWith(SoundCloudTestRunner.class)
public class SecureFileStorageTest {

    @Mock private CryptoOperations operations;
    @Mock private InputStream inputStream;

    private SecureFileStorage storage;
    private final Urn TRACK_URN = Urn.forTrack(123L);

    @Before
    public void setUp() throws Exception {
        storage = new SecureFileStorage(operations);
        when(operations.generateHashForUrn(TRACK_URN)).thenReturn(TRACK_URN.toEncodedString());
    }

    @Test
    public void offlineTracksDirectoryIsCreated() throws IOException, EncryptionException {
        storage.storeTrack(TRACK_URN, inputStream);

        expect(storage.OFFLINE_DIR.exists()).toBeTrue();
    }

    @Test
    public void offlineTrackDirectoryIsReusedWhenAlreadyExists() throws IOException, EncryptionException {
        final SecureFileStorage otherStorage = new SecureFileStorage(operations);

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
        final File file = new File(storage.OFFLINE_DIR, TRACK_URN.toEncodedString()+".enc");

        storage.storeTrack(TRACK_URN, inputStream);

        expect(file.exists()).toBeTrue();
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

    private File createOfflineFile() throws IOException {
        final File file = new File(storage.OFFLINE_DIR, TRACK_URN.toEncodedString()+".enc");
        storage.OFFLINE_DIR.mkdirs();
        file.createNewFile();
        expect(file.exists()).toBeTrue(); // just to ensure we have write permissions
        return file;
    }

    private File getEncryptedFile() {
        return new File(storage.OFFLINE_DIR, TRACK_URN.toEncodedString()+".enc");
    }
}