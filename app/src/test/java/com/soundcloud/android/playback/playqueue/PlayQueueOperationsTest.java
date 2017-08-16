package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.model.Urn.forTrack;
import static com.soundcloud.android.users.User.fromApiUser;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueStorage;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.stations.ApiStation;
import com.soundcloud.android.stations.StationMetadata;
import com.soundcloud.android.stations.StationsRepository;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayQueueOperationsTest extends StorageIntegrationTest {

    private PlayQueueOperations operations;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackItemRepository trackItemRepository;

    @Mock private UserRepository userRepository;
    @Mock private StationsRepository stationsRepository;
    @Mock private PlaylistRepository playlistRepository;
    @Mock private TrackRepository trackRepository;

    @Mock private PlayQueueStorage storage;
    @Captor private ArgumentCaptor<Predicate<PlayQueueItem>> predicateCaptor;

    private final TrackItem trackItem1 = ModelFixtures.trackItem();
    private final TrackItem trackItem2 = ModelFixtures.trackItem();
    private final Urn track1Urn = trackItem1.getUrn();
    private final Urn track2Urn = trackItem2.getUrn();
    private final TrackQueueItem trackQueueItem1 = trackQueueItem(track1Urn);
    private final TrackQueueItem trackQueueItem2 = trackQueueItem(track2Urn);
    private final TrackAndPlayQueueItem trackAndPlayQueueItem1 = new TrackAndPlayQueueItem(trackItem1, trackQueueItem1);
    private final TrackAndPlayQueueItem trackAndPlayQueueItem2 = new TrackAndPlayQueueItem(trackItem2, trackQueueItem2);

    @Before
    public void setUp() throws Exception {

        when(stationsRepository.stationsMetadata(anyList())).thenReturn(Single.just(Collections.EMPTY_LIST));
        when(userRepository.usersInfo(anyList())).thenReturn(Single.just(Collections.EMPTY_LIST));
        when(playlistRepository.withUrns(anyList())).thenReturn(Single.just(Collections.EMPTY_MAP));
        when(trackRepository.fromUrns(anyList())).thenReturn(Single.just(Collections.EMPTY_MAP));


        operations = new PlayQueueOperations(Schedulers.trampoline(),
                                             playQueueManager,
                                             trackItemRepository,
                                             storage,
                                             userRepository,
                                             stationsRepository,
                                             playlistRepository,
                                             trackRepository);
    }

    @Test
    public void getTrackItemsReturnsTrackItemsFromPlayQueue() {
        final List<PlayQueueItem> playQueue = asList(trackQueueItem1, trackQueueItem2);
        final Map<Urn, TrackItem> tracksFromStorage = new HashMap<>();
        tracksFromStorage.put(track1Urn, trackItem1);
        tracksFromStorage.put(track2Urn, trackItem2);
        final List<TrackAndPlayQueueItem> expected = asList(trackAndPlayQueueItem1, trackAndPlayQueueItem2);

        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(playQueue);
        when(trackItemRepository.fromUrns(asList(track1Urn, track2Urn))).thenReturn(Single.just(tracksFromStorage));

        TestObserver<List<TrackAndPlayQueueItem>> testObserver = operations.getTracks().test();

        testObserver.assertValue(expected);
        testObserver.assertTerminated();
    }

    @Test
    public void getTrackItemsDeferPlayQueueItemsLoadingToTheSubscription() {
        when(trackItemRepository.fromUrns(singletonList(track2Urn))).thenReturn(Single.just(singletonMap(track2Urn, trackItem2)));

        final Single<List<TrackAndPlayQueueItem>> operation = operations.getTracks();
        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(singletonList(trackQueueItem2));

        TestObserver<List<TrackAndPlayQueueItem>> testObserver = operation.test();

        for (Throwable throwable : testObserver.errors()) {
            throwable.printStackTrace();
        }

        testObserver.assertValue(singletonList(trackAndPlayQueueItem2));
        testObserver.assertTerminated();
    }

    @Test
    public void getTrackItemsFiltersOutNonTracksFromPlayQueueManager() {
        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(Collections.emptyList());

        operations.getTracks().test();

        verify(playQueueManager).getPlayQueueItems(predicateCaptor.capture());

        final Predicate<PlayQueueItem> predicate = predicateCaptor.getValue();
        assertThat(predicate.apply(TestPlayQueueItem.createTrack(forTrack(1)))).isTrue();
        assertThat(predicate.apply(TestPlayQueueItem.createPlaylist(Urn.forPlaylist(1)))).isFalse();
        assertThat(predicate.apply(TestPlayQueueItem.createTrack(Urn.forAd("dfs:ads", "something")))).isFalse();
        assertThat(predicate.apply(null)).isFalse();
    }

    @Test
    public void getTrackItemsFiltersUnknownTracks() {
        final List<Urn> requestedTracks = asList(track1Urn, track2Urn);
        final List<PlayQueueItem> playQueueItems = asList(trackQueueItem1, trackQueueItem2);
        final Map<Urn, TrackItem> knownTrack = singletonMap(track1Urn, trackItem1);
        final List<TrackAndPlayQueueItem> expectedTrackItems = singletonList(trackAndPlayQueueItem1);

        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(playQueueItems);
        when(trackItemRepository.fromUrns(requestedTracks)).thenReturn(Single.just(knownTrack));

        TestObserver<List<TrackAndPlayQueueItem>> testObserver = operations.getTracks().test();

        testObserver.assertValue(expectedTrackItems);
    }

    @Test
    public void contextTitlesReturnsEmptyValueWhenPlayQueueIsEmpty() {
        TestObserver<Map<Urn, String>> testObserver = operations.getContextTitles().test();

        testObserver.assertValue(emptyMap());
    }

    @Test
    public void getContextTitlesReturnsEmptyValueWhenContextUrnIsAbsent() {
        final TrackQueueItem trackQueueItem = new TrackQueueItem.Builder(forTrack(123L))
                .withPlaybackContext(PlaybackContext.create(PlaySessionSource.EMPTY))
                .build();

        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Collections.singletonList(trackQueueItem));
        storage.store(playQueue);

        TestObserver<Map<Urn, String>> testObserver = operations.getContextTitles().test();
        testObserver.assertValue(emptyMap());
    }

    @Test
    public void getContextTitlesReturnsPlaylistTitle() throws Exception {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        final TrackQueueItem trackQueueItem = createTrackQueueItem(playlist.getUrn(), PlaybackContext.Bucket.PLAYLIST);
        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Collections.singletonList(trackQueueItem));
        storage.store(playQueue);

        when(storage.getContextUrns()).thenReturn(Collections.singletonList(playlist.getUrn()));
        when(playlistRepository.withUrns(anyList())).thenReturn(Single.just(Collections.singletonMap(playlist.getUrn(), Playlist.from(playlist))));

        TestObserver<Map<Urn, String>> testObserver = operations.getContextTitles().test();
        testObserver.assertValue(singletonMap(playlist.getUrn(), playlist.getTitle()));
    }

    @Test
    public void getContextTitlesReturnsProfileTitle() throws Exception {
        final ApiUser user = testFixtures().insertUser();
        final TrackQueueItem trackQueueItem = createTrackQueueItem(user.getUrn(), PlaybackContext.Bucket.PROFILE);

        when(storage.getContextUrns()).thenReturn(Collections.singletonList(fromApiUser(user).urn()));
        when(userRepository.usersInfo(anyList())).thenReturn(Single.just(Collections.singletonList(fromApiUser(user))));

        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Collections.singletonList(trackQueueItem));
        storage.store(playQueue);

        TestObserver<Map<Urn, String>> testObserver = operations.getContextTitles().test();
        testObserver.assertValue(singletonMap(user.getUrn(), user.getUsername()));
    }

    @Test
    public void getContextTitlesReturnsStationTitle() throws Exception {
        final ApiStation station = testFixtures().insertStation();
        final TrackQueueItem trackQueueItem = createTrackQueueItem(station.getUrn(),
                                                                   PlaybackContext.Bucket.TRACK_STATION);

        StationMetadata stationMetadata = StationMetadata.builder()
                                                         .urn(station.getUrn())
                                                         .permalink(Optional.of(station.getPermalink()))
                                                         .imageUrlTemplate(station.getImageUrlTemplate())
                                                         .title(station.getTitle())
                                                         .type(station.getType())
                                                         .build();

        when(storage.getContextUrns()).thenReturn(Collections.singletonList(station.getUrn()));
        when(stationsRepository.stationsMetadata(anyList())).thenReturn(Single.just(Collections.singletonList(stationMetadata)));

        PlayQueue playQueue = PlayQueue.fromPlayQueueItems(Collections.singletonList(trackQueueItem));
        storage.store(playQueue);

        TestObserver<Map<Urn, String>> testObserver = operations.getContextTitles().test();
        testObserver.assertValue(singletonMap(station.getUrn(), station.getTitle()));
    }

    private TrackQueueItem createTrackQueueItem(Urn contextUrn, PlaybackContext.Bucket bucket) {
        return new TrackQueueItem.Builder(forTrack(123L))
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


    public TrackQueueItem trackQueueItem(Urn urn) {
        return TestPlayQueueItem.createTrack(urn);
    }
}
