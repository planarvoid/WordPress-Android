package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DownloadStatePublisherTest extends AndroidUnitTest {

    private final DownloadRequest downloadRequest1 = createDownloadRequest(Urn.forTrack(123L));
    private final DownloadRequest downloadRequest2 = createDownloadRequest(Urn.forTrack(456L));


    private DownloadQueue queue;
    private DownloadStatePublisher publisher;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        queue = new DownloadQueue();
        publisher = new DownloadStatePublisher(eventBus);
    }

    @Test
    public void publishDownloadsRequestedEmitsDownloadRequestedEvent() {
        queue.set(Arrays.asList(downloadRequest1, downloadRequest2));

        publisher.publishDownloadsRequested(queue);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRequested(Arrays.asList(downloadRequest1, downloadRequest2))
        );
    }

    @Test
    public void publishDownloadsRequestedDoesNotEmitNewEventsWhenQueueEmpty() {
        publisher.publishDownloadsRequested(queue);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).isEmpty();
    }

    @Test
    public void publishDoneEmitsNoOfflineEvent() {
        publisher.publishDone();

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(), // one from replay
                CurrentDownloadEvent.idle()  // one published on done
        );
    }

    @Test
    public void publishNotDownloadableStateChangesEmitsNewTrackDownloadedWhenTrackIsRestored() {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Arrays.asList(downloadRequest1),
                Collections.<Urn>emptyList()
        );

        publisher.publishNotDownloadableStateChanges(queue, updates, Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(Arrays.asList(downloadRequest1))
        );
    }

    @Test
    public void publishNotDownloadableStateChangesEmitsDownloadRemovedWhenTrackIsRemoved() {
        final List<Urn> removedDownloads = Arrays.asList(downloadRequest1.track);
        final OfflineContentRequests updates = new OfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                removedDownloads
        );

        publisher.publishNotDownloadableStateChanges(queue, updates, Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRemoved(removedDownloads)
        );
    }

    @Test
    public void publishNotDownloadableStateChangesDoesNotSendDownloadRemovedWhenCurrentlyDownloading() {
        final OfflineContentRequests updates = new OfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Arrays.asList(downloadRequest1.track)
        );

        publisher.publishNotDownloadableStateChanges(queue, updates, downloadRequest1.track);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).isEmpty();
    }

    @Test
    public void publishNotDownloadableStateChangePublishesRequestsRemovedWithOldStateOfTheyQueue() {
        final OfflineContentRequests noOfflineRequest = new OfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList());
        final List<DownloadRequest> previousQueueState = Arrays.asList(downloadRequest1);

        queue.set(previousQueueState);
        publisher.publishNotDownloadableStateChanges(queue, noOfflineRequest, Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRequestRemoved(Arrays.asList(downloadRequest1))
        );
    }

    @Test
    public void publishDownloadingEmitsDownloadingEventWithGivenRequest() {
        publisher.publishDownloading(downloadRequest1);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloading(downloadRequest1)
        );
    }

    @Test
    public void publishDownloadSuccessfulEventsEmitsTrackDownloadedEvent() {
        final DownloadResult result = DownloadResult.success(downloadRequest1);
        publisher.publishDownloadSuccessfulEvents(queue, result);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(Arrays.asList(downloadRequest1))
        );
    }

    @Test
    public void publishDownloadSuccessfulEmitsDownloadRequestEventForRelatedPlaylist() {
        final List<Urn> relatedPlaylists = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(456L));
        final DownloadRequest toBeDownloaded = new DownloadRequest(Urn.forTrack(123L), 0, false, relatedPlaylists);
        final List<DownloadRequest> queueState = Arrays.asList(
                new DownloadRequest(Urn.forTrack(124L), 0, false, relatedPlaylists));

        queue.set(queueState);
        publisher.publishDownloadSuccessfulEvents(queue, DownloadResult.success(toBeDownloaded));

        List<CurrentDownloadEvent> actual = eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD);
        assertThat(actual).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(Arrays.asList(downloadRequest1)),
                CurrentDownloadEvent.downloadRequested(false, relatedPlaylists)
        );
    }

    @Test
    public void publishDownloadErrorEventsEmitsTrackUnavailableEvent() {
        publisher.publishDownloadErrorEvents(queue, DownloadResult.error(downloadRequest1));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.unavailable(Arrays.asList(downloadRequest1))
        );
    }

    @Test
    public void publishDownloadErrorEventsEmitsDownloadRequestEventForRelatedPlaylist() {
        final List<Urn> relatedPlaylists = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(456L));
        final DownloadRequest toBeDownloaded = new DownloadRequest(Urn.forTrack(123L), 0, false, relatedPlaylists);
        final List<DownloadRequest> queueState = Arrays.asList(
                new DownloadRequest(Urn.forTrack(124L), 0, false, relatedPlaylists));

        queue.set(queueState);
        publisher.publishDownloadErrorEvents(queue, DownloadResult.error(toBeDownloaded));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.unavailable(false, Arrays.asList(downloadRequest1.track)),
                CurrentDownloadEvent.downloadRequested(false, relatedPlaylists)
        );
    }

    @Test
    public void publishDownloadCancelEventsEmitsDownloadRemoved() {
        publisher.publishDownloadCancelEvents(queue, DownloadResult.canceled(downloadRequest1));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRemoved(Arrays.asList(downloadRequest1.track))
        );
    }

    @Test
    public void publishDownloadCancelEventsEmitsDownloadedForPlaylistWithNoPendingTracks() {
        final List<Urn> toBeDownloadedPlaylist = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(222L));
        final DownloadRequest toBeDownloaded = new DownloadRequest(Urn.forTrack(123L), 0, false, toBeDownloadedPlaylist);
        final List<DownloadRequest> queueState = Arrays.asList(
                new DownloadRequest(Urn.forTrack(124L), 0, false, Arrays.asList(Urn.forPlaylist(222L))));

        queue.set(queueState);
        publisher.publishDownloadCancelEvents(queue, DownloadResult.canceled(toBeDownloaded));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRemoved(Arrays.asList(downloadRequest1.track)),
                CurrentDownloadEvent.downloaded(false, Arrays.asList(Urn.forPlaylist(123L))));
    }

    private DownloadRequest createDownloadRequest(Urn track) {
        return new DownloadRequest(track, 123456);
    }
}
