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

public class OfflineStatePublisherTest extends AndroidUnitTest {

    private final DownloadRequest downloadRequest1 = createDownloadRequest(Urn.forTrack(123L));
    private final DownloadRequest downloadRequest2 = createDownloadRequest(Urn.forTrack(456L));


    private DownloadQueue queue;
    private OfflineStatePublisher publisher;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        queue = new DownloadQueue();
        publisher = new OfflineStatePublisher(eventBus);
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
        final OfflineContentUpdates updates = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.singletonList(downloadRequest1),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );

        publisher.publishNotDownloadableStateChanges(queue, updates, Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(Collections.singletonList(downloadRequest1))
        );
    }

    @Test
    public void publishNotDownloadableStateChangesEmitsDownloadRemovedWhenTrackIsRemoved() {
        final List<Urn> removedDownloads = Collections.singletonList(downloadRequest1.track);
        final OfflineContentUpdates updates = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
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
        final OfflineContentUpdates updates = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
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
        final OfflineContentUpdates noOfflineRequest = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList());
        final List<DownloadRequest> previousQueueState = Collections.singletonList(downloadRequest1);

        queue.set(previousQueueState);
        publisher.publishNotDownloadableStateChanges(queue, noOfflineRequest, Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRequestRemoved(Collections.singletonList(downloadRequest1))
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
        final DownloadState result = DownloadState.success(downloadRequest1);
        publisher.publishDownloadSuccessfulEvents(queue, result);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(Collections.singletonList(downloadRequest1))
        );
    }

    @Test
    public void publishDownloadSuccessfulEmitsDownloadRequestEventForRelatedPlaylist() {
        final List<Urn> relatedPlaylists = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(456L));
        final DownloadRequest toBeDownloaded = createDownloadRequest(Urn.forTrack(123L), false, relatedPlaylists);
        final List<DownloadRequest> queueState = Collections.singletonList(
                createDownloadRequest(Urn.forTrack(124L), false, relatedPlaylists));

        queue.set(queueState);
        publisher.publishDownloadSuccessfulEvents(queue, DownloadState.success(toBeDownloaded));

        List<CurrentDownloadEvent> actual = eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD);
        assertThat(actual).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(Collections.singletonList(downloadRequest1)),
                CurrentDownloadEvent.downloadRequested(false, relatedPlaylists)
        );
    }

    @Test
    public void publishDownloadErrorEventsEmitsTrackUnavailableEvent() {
        publisher.publishDownloadErrorEvents(queue, DownloadState.error(downloadRequest1));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.unavailable(Collections.singletonList(downloadRequest1))
        );
    }

    @Test
    public void publishDownloadErrorEventsEmitsDownloadRequestEventForRelatedPlaylist() {
        final List<Urn> relatedPlaylists = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(456L));
        final DownloadRequest toBeDownloaded = createDownloadRequest(Urn.forTrack(123L), false, relatedPlaylists);
        final List<DownloadRequest> queueState = Collections.singletonList(
                createDownloadRequest(Urn.forTrack(124L), false, relatedPlaylists));

        queue.set(queueState);
        publisher.publishDownloadErrorEvents(queue, DownloadState.error(toBeDownloaded));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.unavailable(false, Collections.singletonList(downloadRequest1.track)),
                CurrentDownloadEvent.downloadRequested(false, relatedPlaylists)
        );
    }

    @Test
    public void publishDownloadCancelEventsEmitsDownloadRemoved() {
        publisher.publishDownloadCancelEvents(queue, DownloadState.canceled(downloadRequest1));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRemoved(Collections.singletonList(downloadRequest1.track))
        );
    }

    @Test
    public void publishDownloadCancelEventsEmitsDownloadedForPlaylistWithNoPendingTracks() {
        final List<Urn> toBeDownloadedPlaylist = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(222L));
        final DownloadRequest toBeDownloaded = createDownloadRequest(Urn.forTrack(123L), false, toBeDownloadedPlaylist);
        final List<DownloadRequest> queueState = Collections.singletonList(
                createDownloadRequest(Urn.forTrack(124L), false, Collections.singletonList(Urn.forPlaylist(222L))));

        queue.set(queueState);
        publisher.publishDownloadCancelEvents(queue, DownloadState.canceled(toBeDownloaded));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRemoved(Collections.singletonList(downloadRequest1.track)),
                CurrentDownloadEvent.downloaded(false, Collections.singletonList(Urn.forPlaylist(123L))));
    }

    private DownloadRequest createDownloadRequest(Urn track) {
        return new DownloadRequest(track, 123456, "http://wav");
    }

    private DownloadRequest createDownloadRequest(Urn track, boolean inLikes, List<Urn> inPlaylists) {
        return new DownloadRequest(track, 123456, "http://wav", true, inLikes, inPlaylists);
    }
}
