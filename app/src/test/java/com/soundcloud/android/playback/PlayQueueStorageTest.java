package com.soundcloud.android.playback;

import static com.soundcloud.android.model.Urn.forPlaylist;
import static com.soundcloud.android.model.Urn.forTrack;
import static com.soundcloud.android.model.Urn.forUser;
import static com.soundcloud.android.playback.PlaybackContext.Bucket.LISTENING_HISTORY;
import static com.soundcloud.android.storage.Tables.PlayQueue.ENTITY_ID;
import static com.soundcloud.android.storage.Tables.PlayQueue.ENTITY_TYPE;
import static com.soundcloud.android.storage.Tables.PlayQueue.ENTITY_TYPE_PLAYLIST;
import static com.soundcloud.android.storage.Tables.PlayQueue.ENTITY_TYPE_TRACK;
import static com.soundcloud.android.storage.Tables.PlayQueue.PLAYED;
import static com.soundcloud.android.storage.Tables.PlayQueue.QUERY_URN;
import static com.soundcloud.android.storage.Tables.PlayQueue.REPOSTER_ID;
import static com.soundcloud.android.storage.Tables.PlayQueue.SOURCE;
import static com.soundcloud.android.storage.Tables.PlayQueue.SOURCE_URN;
import static com.soundcloud.android.storage.Tables.PlayQueue.SOURCE_VERSION;
import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueItemsEqual;
import static com.soundcloud.java.optional.Optional.of;
import static com.soundcloud.propeller.query.Query.from;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.charts.Chart;
import com.soundcloud.android.playback.TrackQueueItem.Builder;
import com.soundcloud.android.stations.ApiStation;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.test.assertions.QueryAssertions;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;

import android.content.ContentValues;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PlayQueueStorageTest extends StorageIntegrationTest {

    private static final com.soundcloud.propeller.schema.Table PLAY_QUEUE_TABLE = Tables.PlayQueue.TABLE;
    private static final Urn RELATED_ENTITY = Urn.forTrack(987L);
    private static final PlaybackContext PLAYBACK_CONTEXT = PlaybackContext.builder()
                                                                           .bucket(LISTENING_HISTORY)
                                                                           .query(of("some filter"))
                                                                           .urn(of(Urn.forPlaylist(321L)))
                                                                           .build();

    private static final PlaybackContext BUCKET_ONLY_PLAYBACK_CONTEXT = PlaybackContext.builder()
                                                                                       .bucket(LISTENING_HISTORY)
                                                                                       .query(Optional.absent())
                                                                                       .urn(Optional.absent())
                                                                                       .build();

    private PlayQueueStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PlayQueueStorage(propellerRxV2());
    }

    @Test
    public void shouldInsertPlayQueueAndReplaceExistingItems() {
        insertPlayableQueueItem(new Builder(forTrack(1))
                                        .fromSource("existing",
                                                    "existing_version",
                                                    new Urn("existing_sourceUrn"),
                                                    new Urn("existing_queryUrn"))
                                        .withPlaybackContext(PLAYBACK_CONTEXT)
                                        .build());

        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        PlayableQueueItem playableQueueItem1 = new Builder(forTrack(123L))
                .fromSource("source1", "version1", new Urn("sourceUrn1"), new Urn("queryUrn1"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .relatedEntity(RELATED_ENTITY)
                .played(true)
                .build();

        PlayableQueueItem playableQueueItem2 = new Builder(forPlaylist(456L), forUser(456L))
                .fromSource("source2", "version2", new Urn("sourceUrn2"), new Urn("queryUrn2"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .played(false)
                .build();

        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Arrays.asList(playableQueueItem1,
                                                                         playableQueueItem2));

        TestObserver<TxnResult> observer = storage.store(playQueue).test();

        observer.assertValueCount(1);
        observer.assertValue(TxnResult::success);

        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(2);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE)
                                                  .whereEq(ENTITY_ID, 123L)
                                                  .whereEq(ENTITY_TYPE, ENTITY_TYPE_TRACK)
                                                  .whereEq(REPOSTER_ID, Consts.NOT_SET)
                                                  .whereEq(Tables.PlayQueue.RELATED_ENTITY, RELATED_ENTITY.toString())
                                                  .whereEq(SOURCE, "source1")
                                                  .whereEq(SOURCE_VERSION, "version1")
                                                  .whereEq(SOURCE_URN, "sourceUrn1")
                                                  .whereEq(QUERY_URN, "queryUrn1")
                                                  .whereEq(PLAYED, true))).counts(1);

        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE)
                                                  .whereEq(ENTITY_ID, 456L)
                                                  .whereEq(ENTITY_TYPE, Tables.PlayQueue.ENTITY_TYPE_PLAYLIST)
                                                  .whereEq(REPOSTER_ID, 456L)
                                                  .whereEq(SOURCE, "source2")
                                                  .whereEq(SOURCE_VERSION, "version2")
                                                  .whereEq(SOURCE_URN, "sourceUrn2")
                                                  .whereEq(QUERY_URN, "queryUrn2")
                                                  .whereEq(PLAYED, false))).counts(1);
    }

    @Test
    public void shouldSavePlayableQueueItems() {
        final PlayableQueueItem playableQueueItem1 = new Builder(forTrack(1), forUser(1))
                .fromSource("source1", "version1", new Urn("sourceUrn1"), new Urn("queryUrn1"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .build();
        final PlayableQueueItem playableQueueItem2 = new Builder(forPlaylist(2), forUser(2))
                .fromSource("source2", "version2", new Urn("sourceUrn2"), new Urn("queryUrn2"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .build();
        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Arrays.asList(playableQueueItem1,
                                                                         playableQueueItem2));

        storage.store(playQueue).subscribe(new TestObserver<>());

        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(2);
    }

    @Test
    public void shouldDeleteAllPlayQueueItems() {
        insertPlayableQueueItem(new Builder(forTrack(123L), forUser(123L))
                                        .fromSource("source",
                                                    "source_version",
                                                    new Urn("sourceUrn"),
                                                    new Urn("queryUrn"))
                                        .withPlaybackContext(PLAYBACK_CONTEXT)
                                        .build());
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        TestObserver<ChangeResult> testObserver = storage.clear().test();

        testObserver.assertValueCount(1);
        assertThat(testObserver.values().get(0).getNumRowsAffected()).isEqualTo(1);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).isEmpty();
    }

    @Test
    public void shouldLoadAllPlayQueueItems() {
        final PlayableQueueItem expectedItem1 = new Builder(forTrack(123L), forUser(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .relatedEntity(RELATED_ENTITY)
                .build();

        final PlayableQueueItem expectedItem2 = new PlaylistQueueItem.Builder(forPlaylist(456L))
                .fromSource("source", "source_version 2", new Urn("sourceUrn2"), new Urn("queryUrn2"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .relatedEntity(RELATED_ENTITY)
                .build();

        insertPlayableQueueItem(expectedItem1);
        insertPlayableQueueItem(expectedItem2);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(2);

        TestObserver<List<PlayQueueItem>> testObserver = storage.load().test();

        assertPlayQueueItemsEqual(Arrays.asList(expectedItem1, expectedItem2), testObserver.values().get(0));
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutReposter() {
        final PlayableQueueItem expectedItem = new Builder(forTrack(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .relatedEntity(RELATED_ENTITY)
                .build();
        insertPlayableQueueItem(expectedItem);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        TestObserver<List<PlayQueueItem>> testObserver = storage.load().test();

        assertPlayQueueItemsEqual(Collections.singletonList(expectedItem), testObserver.values().get(0));
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutRelatedEntities() {
        final PlayableQueueItem expectedItem = new Builder(forTrack(123L), forTrack(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .build();
        insertPlayableQueueItem(expectedItem);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        TestObserver<List<PlayQueueItem>> testObserver = storage.load().test();

        assertPlayQueueItemsEqual(Collections.singletonList(expectedItem), testObserver.values().get(0));
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutSourceOrQueryUrn() {
        final PlayableQueueItem expectedItem = new Builder(forTrack(123L), forTrack(123L))
                // From a source with no source_urn or query_urn
                .fromSource("source", "source_version")
                .withPlaybackContext(PLAYBACK_CONTEXT)
                .build();
        insertPlayableQueueItem(expectedItem);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        TestObserver<List<PlayQueueItem>> testObserver = storage.load().test();

        assertPlayQueueItemsEqual(Collections.singletonList(expectedItem), testObserver.values().get(0));
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutPlaybackContextUrnOrQuery() {
        final PlayableQueueItem expectedItem = new Builder(forTrack(123L), forTrack(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .relatedEntity(RELATED_ENTITY)
                .withPlaybackContext(BUCKET_ONLY_PLAYBACK_CONTEXT)
                .build();
        insertPlayableQueueItem(expectedItem);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        TestObserver<List<PlayQueueItem>> testObserver = storage.load().test();

        assertPlayQueueItemsEqual(Collections.singletonList(expectedItem), testObserver.values().get(0));
    }

    @Test
    public void contextTitlesReturnsEmptyValueWhenPlayQueueIsEmpty() {
        TestObserver<Map<Urn, String>> testObserver = storage.contextTitles().test();

        testObserver.assertValue(emptyMap());
    }

    @Test
    public void contextTitlesReturnsEmptyValueWhenContextUrnIsAbsent() {
        final TrackQueueItem trackQueueItem = new Builder(forTrack(123L))
                .withPlaybackContext(PlaybackContext.create(PlaySessionSource.EMPTY))
                .build();

        insertPlayableQueueItem(trackQueueItem);

        TestObserver<Map<Urn, String>> testObserver = storage.contextTitles().test();
        testObserver.assertValue(emptyMap());
    }

    @Test
    public void contextTitlesReturnsPlaylistTitle() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        final TrackQueueItem trackQueueItem = createTrackQueueItem(playlist.getUrn(), PlaybackContext.Bucket.PLAYLIST);
        insertPlayableQueueItem(trackQueueItem);

        TestObserver<Map<Urn, String>> testObserver = storage.contextTitles().test();
        testObserver.assertValue(singletonMap(playlist.getUrn(), playlist.getTitle()));
    }

    @Test
    public void contextTitlesReturnsProfileTitle() throws Exception {
        final ApiUser user = testFixtures().insertUser();
        final TrackQueueItem trackQueueItem = createTrackQueueItem(user.getUrn(), PlaybackContext.Bucket.PROFILE);
        insertPlayableQueueItem(trackQueueItem);

        TestObserver<Map<Urn, String>> testObserver = storage.contextTitles().test();
        testObserver.assertValue(singletonMap(user.getUrn(), user.getUsername()));
    }

    @Test
    public void contextTitlesReturnsStationTitle() throws Exception {
        final ApiStation station = testFixtures().insertStation();
        final TrackQueueItem trackQueueItem = createTrackQueueItem(station.getUrn(),
                                                                   PlaybackContext.Bucket.TRACK_STATION);

        insertPlayableQueueItem(trackQueueItem);

        TestObserver<Map<Urn, String>> testObserver = storage.contextTitles().test();
        testObserver.assertValue(singletonMap(station.getUrn(), station.getTitle()));
    }

    @Test
    public void contextTitlesReturnsChartTopMusicTitle() throws Exception {
        final Chart chartTopMusic = testFixtures().insertChart(ChartType.TOP, ChartCategory.MUSIC);
        final TrackQueueItem trackQueueItem = createTrackQueueItem(chartTopMusic.genre(),
                                                                   PlaybackContext.Bucket.CHARTS_TOP);

        insertPlayableQueueItem(trackQueueItem);

        TestObserver<Map<Urn, String>> testObserver = storage.contextTitles().test();
        testObserver.assertValue(singletonMap(chartTopMusic.genre(), chartTopMusic.displayName()));
    }

    @Test
    public void contextTitlesReturnsChartTrendingMusicTitle() throws Exception {
        final Chart chartTopMusic = testFixtures().insertChart(ChartType.TRENDING, ChartCategory.MUSIC);
        final TrackQueueItem trackQueueItem = createTrackQueueItem(chartTopMusic.genre(),
                                                                   PlaybackContext.Bucket.CHARTS_TRENDING);

        insertPlayableQueueItem(trackQueueItem);

        TestObserver<Map<Urn, String>> testObserver = storage.contextTitles().test();
        testObserver.assertValue(singletonMap(chartTopMusic.genre(), chartTopMusic.displayName()));
    }

    @Test
    public void contextTitlesReturnsChartTopAudioTitle() throws Exception {
        final Chart chartTopMusic = testFixtures().insertChart(ChartType.TOP, ChartCategory.AUDIO);
        final TrackQueueItem trackQueueItem = createTrackQueueItem(chartTopMusic.genre(),
                                                                   PlaybackContext.Bucket.CHARTS_TOP);

        insertPlayableQueueItem(trackQueueItem);

        TestObserver<Map<Urn, String>> testObserver = storage.contextTitles().test();
        testObserver.assertValue(singletonMap(chartTopMusic.genre(), chartTopMusic.displayName()));
    }

    @Test
    public void contextTitlesReturnsChartTrendingAudioTitle() throws Exception {
        final Chart chartTopMusic = testFixtures().insertChart(ChartType.TRENDING, ChartCategory.AUDIO);
        final TrackQueueItem trackQueueItem = createTrackQueueItem(chartTopMusic.genre(),
                                                                   PlaybackContext.Bucket.CHARTS_TRENDING);

        insertPlayableQueueItem(trackQueueItem);

        TestObserver<Map<Urn, String>> testObserver = storage.contextTitles().test();
        testObserver.assertValue(singletonMap(chartTopMusic.genre(), chartTopMusic.displayName()));
    }

    private TrackQueueItem createTrackQueueItem(Urn contextUrn, PlaybackContext.Bucket bucket) {
        return new Builder(forTrack(123L))
                .withPlaybackContext(createPlaybackContext(contextUrn, bucket))
                .build();
    }

    private PlaybackContext createPlaybackContext(Urn contextUrn, PlaybackContext.Bucket bucket) {
        return PlaybackContext.builder()
                              .urn(Optional.of(contextUrn))
                              .query(Optional.absent())
                              .bucket(bucket)
                              .build();
    }

    private void insertPlayableQueueItem(PlayableQueueItem playableQueueItem) {
        final PlaybackContext playbackContext = playableQueueItem.getPlaybackContext();
        final ContentValues cv = new ContentValues();

        cv.put(Tables.PlayQueue.ENTITY_ID.name(), playableQueueItem.getUrn().getNumericId());
        cv.put(Tables.PlayQueue.ENTITY_TYPE.name(),
               playableQueueItem.getUrn().isTrack() ? ENTITY_TYPE_TRACK : ENTITY_TYPE_PLAYLIST);
        cv.put(Tables.PlayQueue.SOURCE.name(), playableQueueItem.getSource());
        cv.put(Tables.PlayQueue.SOURCE_VERSION.name(), playableQueueItem.getSourceVersion());
        cv.put(Tables.PlayQueue.SOURCE_URN.name(), playableQueueItem.getSourceUrn().toString());
        cv.put(Tables.PlayQueue.QUERY_URN.name(), playableQueueItem.getQueryUrn().toString());
        cv.put(Tables.PlayQueue.CONTEXT_TYPE.name(), playbackContext.bucket().toString());
        cv.put(Tables.PlayQueue.PLAYED.name(), playableQueueItem.isPlayed());

        if (!Urn.NOT_SET.equals(playableQueueItem.getRelatedEntity())) {
            cv.put(Tables.PlayQueue.RELATED_ENTITY.name(), playableQueueItem.getRelatedEntity().toString());
        }

        if (playableQueueItem.getReposter().isUser()) {
            cv.put(Tables.PlayQueue.REPOSTER_ID.name(), playableQueueItem.getReposter().getNumericId());
        }

        if (playbackContext.urn().isPresent()) {
            cv.put(Tables.PlayQueue.CONTEXT_URN.name(), playbackContext.urn().get().toString());
        }

        if (playbackContext.query().isPresent()) {
            cv.put(Tables.PlayQueue.CONTEXT_QUERY.name(), playbackContext.query().get());
        }

        testFixtures().insertInto(Tables.PlayQueue.TABLE, cv);
    }
}
