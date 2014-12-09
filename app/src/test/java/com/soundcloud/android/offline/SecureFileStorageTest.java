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
    public void offlineTracksDirectoryIsCreated() {
        expect(storage.OFFLINE_DIR.exists()).toBeTrue();
    }

    @Test
    public void offlineTrackDirectoryIsReusedWhenAlreadyExists() throws IOException, EncryptionException {
        final SecureFileStorage otherStorage = new SecureFileStorage(operations);

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
        File file = new File(storage.OFFLINE_DIR, TRACK_URN.toEncodedString());

        storage.storeTrack(TRACK_URN, inputStream);

        expect(file.exists()).toBeTrue();
    }

}