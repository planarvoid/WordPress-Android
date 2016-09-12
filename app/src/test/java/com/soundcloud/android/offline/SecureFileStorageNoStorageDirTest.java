package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.crypto.Encryptor;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

@Config(sdk = 19)
public class SecureFileStorageNoStorageDirTest extends AndroidUnitTest { // just because of logging

    @Mock private CryptoOperations operations;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private InputStream inputStream;
    @Mock private Encryptor.EncryptionProgressListener listener;
    @Mock private Context context;

    private SecureFileStorage storage;
    private final Urn TRACK_URN = Urn.forTrack(123L);
    private final long MINIMUM_SPACE = 5L * 1024 * 1024;

    @Before
    public void setUp() throws Exception {
        when(context.getExternalFilesDirs(anyString())).thenReturn(null);
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
        when(settingsStorage.getStorageLimit()).thenReturn(1024L * 1024 * 1024);
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
        when(settingsStorage.hasStorageLimit()).thenReturn(true);
        when(settingsStorage.getStorageLimit()).thenReturn(MINIMUM_SPACE);

        assertThat(storage.isEnoughMinimumSpace()).isFalse();
    }
}
