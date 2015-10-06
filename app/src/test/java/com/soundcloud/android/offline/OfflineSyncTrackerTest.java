package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncTrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;

public class OfflineSyncTrackerTest {

    private final DownloadRequest DOWNLOAD_REQUEST = ModelFixtures.downloadRequestFromLikes(Urn.forTrack(123));
    private final TestEventBus eventBus = new TestEventBus();

    private OfflineSyncTracker performanceTracker;

    @Before
    public void setUp() {
        performanceTracker = new OfflineSyncTracker(eventBus);
    }

    @Test
    public void testDownloadStarted() {
        performanceTracker.downloadStarted(DOWNLOAD_REQUEST);

        assertTrackingEventSent(
                eventBus.lastEventOn(EventQueue.TRACKING, OfflineSyncTrackingEvent.class),
                OfflineSyncTrackingEvent.KIND_START);
    }

    @Test
    public void testDownloadComplete() {
        performanceTracker.downloadComplete(DOWNLOAD_REQUEST);

        assertTrackingEventSent(
                eventBus.lastEventOn(EventQueue.TRACKING, OfflineSyncTrackingEvent.class),
                OfflineSyncTrackingEvent.KIND_COMPLETE);
    }

    @Test
    public void testDownloadCancelled() {
        performanceTracker.downloadCancelled(DOWNLOAD_REQUEST);

        assertTrackingEventSent(
                eventBus.lastEventOn(EventQueue.TRACKING, OfflineSyncTrackingEvent.class),
                OfflineSyncTrackingEvent.KIND_USER_CANCEL);
    }

    @Test
    public void testDownloadFailed() {
        performanceTracker.downloadFailed(DOWNLOAD_REQUEST);

        assertTrackingEventSent(
                eventBus.lastEventOn(EventQueue.TRACKING, OfflineSyncTrackingEvent.class),
                OfflineSyncTrackingEvent.KIND_FAIL);
    }

    private void assertTrackingEventSent(OfflineSyncTrackingEvent event, String kind) {
        assertThat(event.getKind()).isEqualTo(kind);
        assertThat(event.getTrackUrn()).isEqualTo(DOWNLOAD_REQUEST.getTrack());
        assertThat(event.getTrackOwner()).isEqualTo(DOWNLOAD_REQUEST.getTrackingData().getCreatorUrn());
        assertThat(event.partOfPlaylist()).isEqualTo(DOWNLOAD_REQUEST.getTrackingData().isFromPlaylists());
        assertThat(event.isFromLikes()).isEqualTo(DOWNLOAD_REQUEST.getTrackingData().isFromLikes());
    }
}
