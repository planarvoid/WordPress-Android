package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineState.DOWNLOADED;
import static com.soundcloud.android.offline.OfflineState.DOWNLOADING;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;
import static com.soundcloud.android.offline.OfflineState.REQUESTED;
import static com.soundcloud.android.offline.OfflineState.UNAVAILABLE;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Map;

public class OfflineStatePublisherTest extends AndroidUnitTest {

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);

    @Mock private OfflineStateOperations offlineStateOperations;

    private TestEventBus eventBus;
    private OfflineStatePublisher publisher;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        publisher = new OfflineStatePublisher(eventBus, offlineStateOperations);

        final Map<OfflineState, TrackCollections> collectionsMap = singletonMap(REQUESTED,
                                                                                TrackCollections.create(singletonList(
                                                                                        PLAYLIST), false));
        when(offlineStateOperations.loadTracksCollectionsState(eq(TRACK), any(OfflineState.class))).thenReturn(
                collectionsMap);
    }

    @Test
    public void publishEmptyCollections() {
        publisher.publishEmptyCollections(new ExpectedOfflineContent(Collections.emptyList(),
                                                                     singletonList(PLAYLIST),
                                                                     true,
                                                                     Collections.emptyList()));

        assertEvent(event(0), REQUESTED, true, PLAYLIST);
    }

    @Test
    public void publishDownloading() {
        setTracksCollections(DOWNLOADING);

        publisher.publishDownloading(TRACK);

        assertEvent(event(0), DOWNLOADING, true, TRACK, PLAYLIST);
    }

    @Test
    public void publishRequested() {
        setTracksCollections(REQUESTED);

        publisher.publishRequested(singletonList(TRACK));

        verify(offlineStateOperations).loadTracksCollectionsState(TRACK, REQUESTED);
        assertEvent(event(0), REQUESTED, true, TRACK, PLAYLIST);
    }

    @Test
    public void publishDownloaded() {
        setTracksCollections(REQUESTED);

        publisher.publishDownloaded(TRACK);

        verify(offlineStateOperations).loadTracksCollectionsState(TRACK, DOWNLOADED);
        assertEvent(event(0), REQUESTED, true, PLAYLIST);
        assertEvent(event(1), DOWNLOADED, false, TRACK);
    }

    @Test
    public void publishRemoved() {
        setTracksCollections(UNAVAILABLE);

        publisher.publishRemoved(singletonList(TRACK));

        verify(offlineStateOperations).loadTracksCollectionsState(TRACK, NOT_OFFLINE);
        assertEvent(event(0), NOT_OFFLINE, false, TRACK);
        assertEvent(event(1), UNAVAILABLE, true, PLAYLIST);
    }

    @Test
    public void publishUnavailable() {
        setTracksCollections(DOWNLOADED);

        publisher.publishUnavailable(singletonList(TRACK));

        assertEvent(event(0), DOWNLOADED, true, PLAYLIST);
        assertEvent(event(1), UNAVAILABLE, false, TRACK);
    }

    private OfflineContentChangedEvent event(int location) {
        return eventBus.eventsOn(EventQueue.OFFLINE_CONTENT_CHANGED).get(location);
    }

    private void assertEvent(OfflineContentChangedEvent event,
                             OfflineState state,
                             boolean isLikedTrack,
                             Urn... entities) {
        assertThat(event.state).isEqualTo(state);
        assertThat(event.entities).containsOnly(entities);
        assertThat(event.isLikedTrackCollection).isEqualTo(isLikedTrack);
    }

    private void setTracksCollections(OfflineState state) {
        when(offlineStateOperations.loadTracksCollectionsState(eq(TRACK), any(OfflineState.class)))
                .thenReturn(singletonMap(state, TrackCollections.create(singletonList(PLAYLIST), true)));
    }

}
