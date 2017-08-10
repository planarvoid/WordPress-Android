package com.soundcloud.android.offline;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.crypto.Encryptor;
import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

@RunWith(MockitoJUnitRunner.class)
@Config(sdk = 19)
public class SecureFileStorageNoStorageDirTest { // just because of logging

    @Mock private CryptoOperations operations;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private InputStream inputStream;
    @Mock private Encryptor.EncryptionProgressListener listener;
    @Mock private Context context;

    private SecureFileStorage storage;
    private final Urn TRACK_URN = Urn.forTrack(123L);

    @Before
    public void setUp() throws Exception {
        storage = new SecureFileStorage(operations, settingsStorage, context);
    }

    @Test(expected=IOException.class)
    public void throwsIOExceptionWhenTryingToStoreTrack() throws IOException, EncryptionException {
        storage.storeTrack(TRACK_URN, inputStream, listener);
    }

    @Test
    public void returnsEmptyUriForOfflineTrackUri() throws Exception {
        assertThat(storage.getFileUriForOfflineTrack(TRACK_URN)).isEqualTo(Uri.EMPTY);
    }

    @Test
    public void shouldBeEnoughSpaceForTrackInStorageHappyCase() throws Exception {
        assertThat(storage.isEnoughSpace(0)).isFalse();
    }

    @Test
    public void deleteTrackRemovesDoesNotCrash() throws Exception {
        storage.deleteAllTracks();
    }

    @Test
    public void returnsUsedStorageIsZero() throws Exception {
        assertThat(storage.getStorageUsed()).isEqualTo(0L);
    }

    @Test
    public void shouldBeEnoughMinimumSpaceIsFalset() throws Exception {
        assertThat(storage.isEnoughMinimumSpace()).isFalse();
    }
}
