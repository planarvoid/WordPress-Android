package com.soundcloud.android.offline;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OfflineStatePublisherTest extends AndroidUnitTest {

    private final DownloadRequest downloadRequest1 = ModelFixtures.downloadRequestFromLikes(Urn.forTrack(123L));
    private final DownloadRequest downloadRequest2 = ModelFixtures.downloadRequestFromLikes(Urn.forTrack(456L));

    private DownloadQueue queue;
    private OfflineStatePublisher publisher;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        queue = new DownloadQueue();
        publisher = new OfflineStatePublisher(eventBus);
        publisher.setUpdates(emptyUpdates());
    }

    @Test
    public void publishDownloadsRequestedEmitsDownloadRequestedEvent() {
        queue.set(Arrays.asList(downloadRequest1, downloadRequest2));

        publisher.publishDownloadsRequested(queue.getRequests());

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRequested(Arrays.asList(downloadRequest1, downloadRequest2))
        );
    }

    @Test
    public void publishDownloadsRequestedDoesNotEmitNewEventsWhenQueueEmpty() {
        publisher.publishDownloadsRequested(queue.getRequests());

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
                singletonList(downloadRequest1),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );

        publisher.setUpdates(updates);
        publisher.publishNotDownloadableStateChanges(queue, Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(singletonList(downloadRequest1))
        );
    }

    @Test
    public void publishNotDownloadableStateChangesEmitsDownloadRemovedWhenTrackIsRemoved() {
        final List<Urn> removedDownloads = singletonList(downloadRequest1.getTrack());
        final OfflineContentUpdates updates = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                removedDownloads
        );

        publisher.setUpdates(updates);
        publisher.publishNotDownloadableStateChanges(queue, Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRemoved(removedDownloads)
        );
    }

    @Test
    public void publishNotDownloadableStateChangesEmitsTrackUnavailableWithCreatorOptOutTracks() {
        final List<DownloadRequest> creatorOptOut = singletonList(creatorOptOutRequest(Urn.forTrack(123L)));
        final OfflineContentUpdates updates = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                creatorOptOut,
                Collections.<Urn>emptyList()
        );

        publisher.setUpdates(updates);
        publisher.publishNotDownloadableStateChanges(queue, Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.unavailable(creatorOptOut)
        );
    }

    @Test
    public void publishNotDownloadableStateChangesDoesNotSendDownloadRemovedWhenCurrentlyDownloading() {
        final OfflineContentUpdates updates = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Arrays.asList(downloadRequest1.getTrack())
        );

        publisher.setUpdates(updates);
        publisher.publishNotDownloadableStateChanges(queue, downloadRequest1.getTrack());

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).isEmpty();
    }

    @Test
    public void publishNotDownloadableStateChangePublishesRequestsRemovedWithOldStateOfTheyQueue() {
        final List<DownloadRequest> previousQueueState = singletonList(downloadRequest1);

        queue.set(previousQueueState);
        publisher.setUpdates(emptyUpdates());
        publisher.publishNotDownloadableStateChanges(queue, Urn.NOT_SET);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRequestRemoved(singletonList(downloadRequest1))
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
        publisher.setUpdates(emptyUpdates());
        publisher.publishDownloadSuccessfulEvents(queue, result);

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(singletonList(downloadRequest1))
        );
    }

    @Test
    public void publishDownloadSuccessfulEmitsDownloadRequestEventForRelatedPlaylist() {
        final List<Urn> relatedPlaylists = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(456L));
        final DownloadRequest toBeDownloaded = createDownloadRequest(Urn.forTrack(123L), false, relatedPlaylists);
        final List<DownloadRequest> queueState = singletonList(
                createDownloadRequest(Urn.forTrack(124L), false, relatedPlaylists));

        queue.set(queueState);
        publisher.publishDownloadSuccessfulEvents(queue, DownloadState.success(toBeDownloaded));

        List<CurrentDownloadEvent> actual = eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD);
        assertThat(actual).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloaded(toBeDownloaded.isLiked(), singletonList(toBeDownloaded.getTrack())),
                CurrentDownloadEvent.downloadRequested(false, relatedPlaylists)
        );
    }

    @Test
    public void publishDownloadErrorEventsEmitsDownloadRequestEventForRelatedPlaylist() {
        final List<Urn> relatedPlaylists = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(456L));
        final DownloadRequest toBeDownloaded = createDownloadRequest(Urn.forTrack(123L), false, relatedPlaylists);
        final List<DownloadRequest> queueState = singletonList(
                createDownloadRequest(Urn.forTrack(124L), false, relatedPlaylists));

        queue.set(queueState);
        publisher.publishDownloadErrorEvents(queue, DownloadState.error(toBeDownloaded));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRequested(false, relatedPlaylists)
        );
    }

    @Test
    public void publishDownloadCancelEventsEmitsDownloadRemoved() {
        publisher.publishDownloadCancelEvents(queue, DownloadState.canceled(downloadRequest1));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRemoved(singletonList(downloadRequest1.getTrack())),
                CurrentDownloadEvent.downloaded(downloadRequest1.isLiked(), downloadRequest1.getPlaylists())
        );
    }

    @Test
    public void publishDownloadCancelEventsEmitsDownloadedForPlaylistWithNoPendingTracks() {
        final List<Urn> toBeDownloadedPlaylist = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(222L));
        final DownloadRequest toBeDownloaded = createDownloadRequest(Urn.forTrack(123L), false, toBeDownloadedPlaylist);
        final List<DownloadRequest> queueState = singletonList(
                createDownloadRequest(Urn.forTrack(124L), false, singletonList(Urn.forPlaylist(222L))));

        queue.set(queueState);
        publisher.publishDownloadCancelEvents(queue, DownloadState.canceled(toBeDownloaded));

        assertThat(eventBus.eventsOn(EventQueue.CURRENT_DOWNLOAD)).containsExactly(
                CurrentDownloadEvent.idle(),
                CurrentDownloadEvent.downloadRemoved(singletonList(downloadRequest1.getTrack())),
                CurrentDownloadEvent.downloaded(false, singletonList(Urn.forPlaylist(123L))));
    }

    private DownloadRequest createDownloadRequest(Urn trackUrn, boolean inLikes, List<Urn> inPlaylists) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        track.setUrn(trackUrn);
        return ModelFixtures.downloadRequestFromPlaylists(track, inLikes, inPlaylists);
    }

    private DownloadRequest creatorOptOutRequest(Urn track) {
        return ModelFixtures.creatorOptOutRequest(track);
    }

    private OfflineContentUpdates emptyUpdates() {
        return new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList());
    }
}
