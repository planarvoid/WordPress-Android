package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.TrackingMetadata;
import org.junit.Test;

public class OfflinePerformanceEventTest {

    private final Urn TRACK_URN = Urn.forTrack(123);
    private final TrackingMetadata TRACK_CONTEXT = getTrackContext();

    @Test
    public void createsSyncCompleteOfflineEvent() {
        final OfflinePerformanceEvent event = OfflinePerformanceEvent.fromCompleted(TRACK_URN, TRACK_CONTEXT);

        assertThat(event.kind()).isEqualTo(OfflinePerformanceEvent.Kind.KIND_COMPLETE);
        assertThatTrackContextValuesAreEqual(event, TRACK_URN, TRACK_CONTEXT);
    }

    @Test
    public void createsSyncStartOfflineEvent() {
        final OfflinePerformanceEvent event = OfflinePerformanceEvent.fromStarted(TRACK_URN, TRACK_CONTEXT);

        assertThat(event.kind()).isEqualTo(OfflinePerformanceEvent.Kind.KIND_START);
        assertThatTrackContextValuesAreEqual(event, TRACK_URN, TRACK_CONTEXT);
    }

    @Test
    public void createsSyncFailedOfflineEvent() {
        final OfflinePerformanceEvent event = OfflinePerformanceEvent.fromFailed(TRACK_URN, TRACK_CONTEXT);

        assertThat(event.kind()).isEqualTo(OfflinePerformanceEvent.Kind.KIND_FAIL);
        assertThatTrackContextValuesAreEqual(event, TRACK_URN, TRACK_CONTEXT);
    }

    @Test
    public void createsSyncCancelOfflineEvent() {
        final OfflinePerformanceEvent event = OfflinePerformanceEvent.fromCancelled(TRACK_URN, TRACK_CONTEXT);

        assertThat(event.kind()).isEqualTo(OfflinePerformanceEvent.Kind.KIND_USER_CANCEL);
        assertThatTrackContextValuesAreEqual(event, TRACK_URN, TRACK_CONTEXT);
    }

    @Test
    public void createSyncErrorFromStorageInaccessibleEvent() {
        final OfflinePerformanceEvent event = OfflinePerformanceEvent.fromStorageInaccessible(TRACK_URN, TRACK_CONTEXT);

        assertThat(event.kind()).isEqualTo(OfflinePerformanceEvent.Kind.KIND_STORAGE_INACCESSIBLE);
        assertThatTrackContextValuesAreEqual(event, TRACK_URN, TRACK_CONTEXT);
    }

    @Test
    public void createSyncErrorFromStorageLimitReachedEvent() {
        final OfflinePerformanceEvent event = OfflinePerformanceEvent.fromStorageLimit(TRACK_URN, TRACK_CONTEXT);

        assertThat(event.kind()).isEqualTo(OfflinePerformanceEvent.Kind.KIND_STORAGE_LIMIT);
        assertThatTrackContextValuesAreEqual(event, TRACK_URN, TRACK_CONTEXT);
    }

    private void assertThatTrackContextValuesAreEqual(OfflinePerformanceEvent event,
                                                      Urn track,
                                                      TrackingMetadata context) {
        assertThat(event.trackUrn()).isEqualTo(track);
        assertThat(event.trackOwner()).isEqualTo(context.getCreatorUrn());
        assertThat(event.isFromLikes()).isEqualTo(context.isFromLikes());
        assertThat(event.partOfPlaylist()).isEqualTo(context.isFromPlaylists());
    }

    private static TrackingMetadata getTrackContext() {
        return new TrackingMetadata(Urn.forUser(123L), false, false);
    }
}
