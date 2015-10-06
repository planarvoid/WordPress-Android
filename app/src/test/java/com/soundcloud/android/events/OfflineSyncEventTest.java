package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineTrackContext;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class OfflineSyncEventTest {

    @Test
    public void createsDesyncOfflineEvent() {
        final OfflineTrackContext context = getTrackContext();

        final OfflineSyncEvent event = OfflineSyncEvent.fromDesync(context);

        assertThat(event.getKind()).isEqualTo(OfflineSyncEvent.KIND_DESYNC);
        assertThat(event.getStage()).isEqualTo(OfflineSyncEvent.STAGE_COMPLETE);
        assertThatTrackContextValuesAreEqual(event, context);
    }

    @Test
    public void createstSyncCompleteOfflineEvent() {
        final OfflineTrackContext context = getTrackContext();

        final OfflineSyncEvent event = OfflineSyncEvent.fromSyncComplete(context);

        assertThat(event.getKind()).isEqualTo(OfflineSyncEvent.KIND_SYNC);
        assertThat(event.getStage()).isEqualTo(OfflineSyncEvent.STAGE_COMPLETE);
        assertThatTrackContextValuesAreEqual(event, context);
    }

    @Test
    public void createstSyncStartOfflineEvent() {
        final OfflineTrackContext context = getTrackContext();

        final OfflineSyncEvent event = OfflineSyncEvent.fromSyncStart(context);

        assertThat(event.getKind()).isEqualTo(OfflineSyncEvent.KIND_SYNC);
        assertThat(event.getStage()).isEqualTo(OfflineSyncEvent.STAGE_START);
        assertThatTrackContextValuesAreEqual(event, context);
    }

    @Test
    public void createstSyncFailedOfflineEvent() {
        final OfflineTrackContext context = getTrackContext();

        final OfflineSyncEvent event = OfflineSyncEvent.fromSyncFail(context);

        assertThat(event.getKind()).isEqualTo(OfflineSyncEvent.KIND_SYNC);
        assertThat(event.getStage()).isEqualTo(OfflineSyncEvent.STAGE_FAIL);
        assertThatTrackContextValuesAreEqual(event, context);
    }

    private void assertThatTrackContextValuesAreEqual(OfflineSyncEvent event, OfflineTrackContext context) {
        assertThat(event.getTrackUrn()).isEqualTo(context.getTrack());
        assertThat(event.getTrackOwner()).isEqualTo(context.getCreator());
        assertThat(event.inLikes()).isEqualTo(context.inLikes());
        assertThat(event.inPlaylist()).isEqualTo(!context.inPlaylists().isEmpty());
    }

    private OfflineTrackContext getTrackContext() {
        return OfflineTrackContext.create(Urn.forTrack(123L), Urn.forUser(123L), Collections.EMPTY_LIST, false);
    }

}