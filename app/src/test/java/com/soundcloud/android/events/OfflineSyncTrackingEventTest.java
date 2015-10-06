package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.TrackingMetadata;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OfflineSyncTrackingEventTest {

    private final Urn TRACK_URN = Urn.forTrack(123);
    private final TrackingMetadata TRACK_CONTEXT = getTrackContext();

    @Test
    public void createsSyncCompleteOfflineEvent() {
        final OfflineSyncTrackingEvent event = OfflineSyncTrackingEvent.fromCompleted(TRACK_URN, TRACK_CONTEXT);

        assertThat(event.getKind()).isEqualTo(OfflineSyncTrackingEvent.KIND_COMPLETE);
        assertThatTrackContextValuesAreEqual(event, TRACK_URN, TRACK_CONTEXT);
    }

    @Test
    public void createsSyncStartOfflineEvent() {
        final OfflineSyncTrackingEvent event = OfflineSyncTrackingEvent.fromStarted(TRACK_URN, TRACK_CONTEXT);

        assertThat(event.getKind()).isEqualTo(OfflineSyncTrackingEvent.KIND_START);
        assertThatTrackContextValuesAreEqual(event, TRACK_URN, TRACK_CONTEXT);
    }

    @Test
    public void createsSyncFailedOfflineEvent() {
        final OfflineSyncTrackingEvent event = OfflineSyncTrackingEvent.fromFailed(TRACK_URN, TRACK_CONTEXT);

        assertThat(event.getKind()).isEqualTo(OfflineSyncTrackingEvent.KIND_FAIL);
        assertThatTrackContextValuesAreEqual(event, TRACK_URN, TRACK_CONTEXT);
    }

    @Test
    public void createsSyncCancelOfflineEvent() {
        final OfflineSyncTrackingEvent event = OfflineSyncTrackingEvent.fromCancelled(TRACK_URN, TRACK_CONTEXT);

        assertThat(event.getKind()).isEqualTo(OfflineSyncTrackingEvent.KIND_USER_CANCEL);
        assertThatTrackContextValuesAreEqual(event, TRACK_URN, TRACK_CONTEXT);
    }

    private void assertThatTrackContextValuesAreEqual(OfflineSyncTrackingEvent event, Urn track, TrackingMetadata context) {
        assertThat(event.getTrackUrn()).isEqualTo(track);
        assertThat(event.getTrackOwner()).isEqualTo(context.getCreatorUrn());
        assertThat(event.isFromLikes()).isEqualTo(context.isFromLikes());
        assertThat(event.partOfPlaylist()).isEqualTo(context.isFromPlaylists());
    }

    private static TrackingMetadata getTrackContext() {
        return new TrackingMetadata(Urn.forUser(123L), false, false);
    }
}
