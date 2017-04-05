package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentLocation.DEVICE_STORAGE;
import static com.soundcloud.android.offline.OfflineContentLocation.SD_CARD;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;

public class OfflineStorageOperationsTest extends AndroidUnitTest {

    @Mock private CryptoOperations cryptoOperations;
    @Mock private OfflineSettingsStorage settingsStorage;

    private TestEventBus eventBus = new TestEventBus();
    private OfflineStorageOperations operations;

    @Before
    public void setUp() {
        operations = new OfflineStorageOperations(context(), cryptoOperations, settingsStorage, eventBus);
    }

    @Test
    public void shouldNotUpdateOfflineContentOnSdCard() {
        when(settingsStorage.getOfflineContentLocation()).thenReturn(DEVICE_STORAGE);

        operations.updateOfflineContentOnSdCard();

        assertThat(getNextStartedService()).isNull();
    }

    @Test
    public void shouldUpdateOfflineContentOnSdCard() {
        when(settingsStorage.getOfflineContentLocation()).thenReturn(SD_CARD);

        operations.updateOfflineContentOnSdCard();

        Intent nextStartedService = getNextStartedService();
        assertThat(nextStartedService).isNotNull();
        Assertions.assertThat(nextStartedService).containsAction("action_start_download")
                  .startsService(OfflineContentService.class);
    }

    @Test
    public void tracksSdCardAvailabilityIfNeverRecordedBefore() {
        when(settingsStorage.hasReportedSdCardAvailability()).thenReturn(false);

        operations.init();

        final OfflineInteractionEvent event = eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent.class);
        assertThat(event.impressionName().get()).isEqualTo(OfflineInteractionEvent.Kind.KIND_OFFLINE_SD_AVAILABLE);
        assertThat(event.isEnabled().get()).isFalse();
    }

    @Test
    public void doesNotTrackSdCardAvailabilityIfAlreadyRecorded() {
        when(settingsStorage.hasReportedSdCardAvailability()).thenReturn(true);

        operations.init();

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

}
