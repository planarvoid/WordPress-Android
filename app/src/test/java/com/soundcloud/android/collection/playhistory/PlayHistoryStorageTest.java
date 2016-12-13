package com.soundcloud.android.collection.playhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.Property;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayHistoryStorageTest extends StorageIntegrationTest {

    private static final Property[] EXPECTED_PROPERTIES = {
            TrackProperty.URN,
            PlayableProperty.TITLE,
            TrackProperty.SNIPPET_DURATION,
            TrackProperty.FULL_DURATION,
            TrackProperty.PLAY_COUNT,
            TrackProperty.COMMENTS_COUNT,
            PlayableProperty.LIKES_COUNT,
            PlayableProperty.REPOSTS_COUNT,
            TrackProperty.MONETIZABLE,
            TrackProperty.BLOCKED,
            TrackProperty.SNIPPED,
            TrackProperty.SUB_HIGH_TIER,
            TrackProperty.MONETIZATION_MODEL,
            PlayableProperty.PERMALINK_URL,
            PlayableProperty.IS_PRIVATE,
            PlayableProperty.CREATED_AT,
            EntityProperty.IMAGE_URL_TEMPLATE,
            TrackProperty.WAVEFORM_URL,
            PlayableProperty.CREATOR_NAME,
            PlayableProperty.CREATOR_URN
    };

    private PlayHistoryStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PlayHistoryStorage(propeller());
    }

    @Test
    public void loadTracksReturnsTrackItemsSortedInReverseTimestampOrder()  {
        final TrackItem expected1 = insertTrackWithPlayHistory(1000L);
        final TrackItem expected2 = insertTrackWithPlayHistory(2000L);

        final List<TrackItem> tracks = storage.loadTracks(10).toList().toBlocking().single();

        assertThat(tracks.size()).isEqualTo(2);
        assertSameTrackItem(expected2, tracks.get(0));
        assertSameTrackItem(expected1, tracks.get(1));
    }

    @Test
    public void loadTracksGroupsByLastPlayed() {
        final TrackItem track1 = insertTrackWithPlayHistory(1000L);
        final TrackItem track2 = insertTrackWithPlayHistory(2000L);
        final TrackItem track3 = insertTrackWithPlayHistory(3000L);

        // played on another device after sync
        insertPlayHistory(track3.getUrn(), 4000L);
        insertPlayHistory(track2.getUrn(), 1500L);
        insertPlayHistory(track1.getUrn(), 2500L);

        final List<TrackItem> tracks = storage.loadTracks(10).toList().toBlocking().single();

        assertThat(tracks.size()).isEqualTo(3);
        assertSameTrackItem(track3, tracks.get(0));
        assertSameTrackItem(track1, tracks.get(1));
        assertSameTrackItem(track2, tracks.get(2));
    }

    @Test
    public void loadTracksSetDownloadState() {
        final TrackItem expected = insertTrackWithPlayHistory(1000L);
        testFixtures().insertCompletedTrackDownload(expected.getUrn(), 1000L, 2000L);

        final TrackItem actual = storage.loadTracks(10).first().toBlocking().single();

        assertThat(actual.getOfflineState()).isEqualTo(OfflineState.DOWNLOADED);
    }

    @Test
    public void loadTracksReturnsTrackItems() {
        final TrackItem expected = insertTrackWithPlayHistory(1000L);

        TrackItem actual = storage.loadTracks(10).first().toBlocking().single();

        assertSameTrackItem(expected, actual);
    }

    @Test
    public void loadUnSyncedPlayHistoryReturnsOnlyUnsynced() {
        final Urn trackUrn = Urn.forTrack(123L);
        testFixtures().insertUnsyncedPlayHistory(1000L, trackUrn);
        testFixtures().insertPlayHistory(2000L, trackUrn);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadUnSyncedPlayHistory();

        assertThat(playHistoryRecords.size()).isEqualTo(1);
        assertThat(playHistoryRecords.get(0).trackUrn()).isEqualTo(trackUrn);
        assertThat(playHistoryRecords.get(0).timestamp()).isEqualTo(1000L);
    }

    @Test
    public void loadSyncedPlayHistoryReturnsOnlySynced() {
        final Urn trackUrn = Urn.forTrack(123L);
        testFixtures().insertUnsyncedPlayHistory(1000L, trackUrn);
        testFixtures().insertPlayHistory(2000L, trackUrn);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadSyncedPlayHistory();

        assertThat(playHistoryRecords.size()).isEqualTo(1);
        assertThat(playHistoryRecords.get(0).trackUrn()).isEqualTo(trackUrn);
        assertThat(playHistoryRecords.get(0).timestamp()).isEqualTo(2000L);
    }

    @Test
    public void setSyncedUpdatesEntries() {
        final Urn trackUrn = Urn.forTrack(123L);
        final Urn trackUrn2 = Urn.forTrack(234L);
        testFixtures().insertUnsyncedPlayHistory(1000L, trackUrn);
        testFixtures().insertUnsyncedPlayHistory(1500L, trackUrn2);
        testFixtures().insertPlayHistory(2000L, trackUrn);

        storage.setSynced(Arrays.asList(
                PlayHistoryRecord.create(1000L, trackUrn, Urn.NOT_SET),
                PlayHistoryRecord.create(1500L, trackUrn2, Urn.NOT_SET)));

        assertThat(storage.loadUnSyncedPlayHistory().size()).isEqualTo(0);
    }

    @Test
    public void removePlayHistoryRemovesEntries() {
        final TrackItem trackItem1 = insertTrackWithPlayHistory(2000L);
        final TrackItem trackItem2 = insertTrackWithPlayHistory(3000L);

        storage.removePlayHistory(Collections.singletonList(
                PlayHistoryRecord.create(2000L, trackItem1.getUrn(), Urn.NOT_SET)));

        final List<TrackItem> existingTracks = storage.loadTracks(10).toList().toBlocking().single();

        assertThat(existingTracks.size()).isEqualTo(1);
        assertSameTrackItem(trackItem2, existingTracks.get(0));
    }

    @Test
    public void insertPlayHistoryAddsEntriesAsSynced() {
        insertPlayHistory(Urn.forTrack(123), 1000L);
        insertPlayHistory(Urn.forTrack(234), 2000L);

        storage.insertPlayHistory(Collections.singletonList(
                PlayHistoryRecord.create(3000L, Urn.forTrack(123), Urn.NOT_SET)));

        assertThat(storage.loadSyncedPlayHistory().size()).isEqualTo(3);
    }

    @Test
    public void loadPlayHistoryForPlaybackGetsOnlyUrnsWithoutDuplicates() {
        final Urn urn1 = Urn.forTrack(123);
        final Urn urn2 = Urn.forTrack(234);
        insertPlayHistory(urn1, 1000L);
        insertPlayHistory(urn2, 2000L);
        insertPlayHistory(urn1, 3000L);
        // from syncer
        insertPlayHistory(urn1, 1500L);

        final List<Urn> urns = storage.loadPlayHistoryForPlayback().toList().toBlocking().single();

        assertThat(urns).containsExactly(urn1, urn2);
    }

    @Test
    public void clearClearsTable() {
        testFixtures().insertUnsyncedPlayHistory(1500L, Urn.forTrack(123L));
        testFixtures().insertPlayHistory(2000L, Urn.forTrack(234L));

        storage.clear();

        databaseAssertions().assertPlayHistoryCount(0);
    }

    private void assertSameTrackItem(TrackItem expected, TrackItem actual) {
        assertThat(expected.getSource().slice(EXPECTED_PROPERTIES))
                .isEqualTo(actual.getSource().slice(EXPECTED_PROPERTIES));
    }

    private TrackItem insertTrackWithPlayHistory(long timestamp) {
        final TrackItem trackItem = TrackItem.from(testFixtures().insertTrack());
        insertPlayHistory(trackItem.getUrn(), timestamp);
        return trackItem;
    }

    private void insertPlayHistory(Urn urn, long timestamp) {
        testFixtures().insertPlayHistory(timestamp, urn);
    }

}
