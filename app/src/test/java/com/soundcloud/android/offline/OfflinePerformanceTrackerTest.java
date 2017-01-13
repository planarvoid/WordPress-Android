package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;

public class OfflinePerformanceTrackerTest extends AndroidUnitTest {

    private final DownloadRequest DOWNLOAD_REQUEST = ModelFixtures.downloadRequestFromLikes(Urn.forTrack(123));
    private final TestEventBus eventBus = new TestEventBus();

    private OfflinePerformanceTracker performanceTracker;

    @Before
    public void setUp() {
        performanceTracker = new OfflinePerformanceTracker(eventBus);
    }

    @Test
    public void testDownloadStarted() {
        performanceTracker.downloadStarted(DOWNLOAD_REQUEST);

        assertTrackingEventSent(
                eventBus.lastEventOn(EventQueue.TRACKING, OfflinePerformanceEvent.class),
                OfflinePerformanceEvent.Kind.KIND_START);
    }

    @Test
    public void testDownloadComplete() {
        performanceTracker.downloadComplete(DownloadState.success(DOWNLOAD_REQUEST));

        assertTrackingEventSent(
                eventBus.lastEventOn(EventQueue.TRACKING, OfflinePerformanceEvent.class),
                OfflinePerformanceEvent.Kind.KIND_COMPLETE);
    }

    @Test
    public void testDownloadCancelled() {
        performanceTracker.downloadCancelled(DownloadState.canceled(DOWNLOAD_REQUEST));

        assertTrackingEventSent(
                eventBus.lastEventOn(EventQueue.TRACKING, OfflinePerformanceEvent.class),
                OfflinePerformanceEvent.Kind.KIND_USER_CANCEL);
    }

    @Test
    public void testDownloadFailed() {
        performanceTracker.downloadFailed(DownloadState.error(DOWNLOAD_REQUEST));

        assertTrackingEventSent(
                eventBus.lastEventOn(EventQueue.TRACKING, OfflinePerformanceEvent.class),
                OfflinePerformanceEvent.Kind.KIND_FAIL);
    }

    @Test
    public void testStorageLimitReachedError() {
        performanceTracker.downloadFailed(DownloadState.notEnoughSpace(DOWNLOAD_REQUEST));

        assertTrackingEventSent(
                eventBus.lastEventOn(EventQueue.TRACKING, OfflinePerformanceEvent.class),
                OfflinePerformanceEvent.Kind.KIND_STORAGE_LIMIT);
    }

    @Test
    public void testStorageLimitReachedErrorForMinimumSpace() {
        performanceTracker.downloadFailed(DownloadState.notEnoughMinimumSpace(DOWNLOAD_REQUEST));

        assertTrackingEventSent(
                eventBus.lastEventOn(EventQueue.TRACKING, OfflinePerformanceEvent.class),
                OfflinePerformanceEvent.Kind.KIND_STORAGE_LIMIT);
    }

    private void assertTrackingEventSent(OfflinePerformanceEvent event, OfflinePerformanceEvent.Kind kind) {
        assertThat(event.kind()).isEqualTo(kind);
        assertThat(event.trackUrn()).isEqualTo(DOWNLOAD_REQUEST.getUrn());
        assertThat(event.trackOwner()).isEqualTo(DOWNLOAD_REQUEST.getTrackingData().getCreatorUrn());
        assertThat(event.partOfPlaylist()).isEqualTo(DOWNLOAD_REQUEST.getTrackingData().isFromPlaylists());
        assertThat(event.isFromLikes()).isEqualTo(DOWNLOAD_REQUEST.getTrackingData().isFromLikes());
    }
}
