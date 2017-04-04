package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentLocation.DEVICE_STORAGE;
import static com.soundcloud.android.offline.OfflineContentLocation.SD_CARD;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;

public class OfflineStorageOperationsTest extends AndroidUnitTest {

    @Mock private CryptoOperations cryptoOperations;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;

    private OfflineStorageOperations offlineStorageOperations;

    @Before
    public void setUp() {
        initMocks(this);
        offlineStorageOperations = new OfflineStorageOperations(cryptoOperations,
                                                                offlineSettingsStorage,
                                                                context());
    }

    @Test
    public void shouldNotUpdateOfflineContentOnSdCard() {
        when(offlineSettingsStorage.getOfflineContentLocation()).thenReturn(DEVICE_STORAGE);

        offlineStorageOperations.updateOfflineContentOnSdCard();

        assertThat(getNextStartedService()).isNull();
    }

    @Test
    public void shouldUpdateOfflineContentOnSdCard() {
        when(offlineSettingsStorage.getOfflineContentLocation()).thenReturn(SD_CARD);

        offlineStorageOperations.updateOfflineContentOnSdCard();

        Intent nextStartedService = getNextStartedService();
        assertThat(nextStartedService).isNotNull();
        Assertions.assertThat(nextStartedService).containsAction("action_start_download")
                  .startsService(OfflineContentService.class);
    }
}
