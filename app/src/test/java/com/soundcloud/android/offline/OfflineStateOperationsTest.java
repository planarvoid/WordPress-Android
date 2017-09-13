package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineState.DOWNLOADED;
import static com.soundcloud.android.offline.OfflineState.DOWNLOADING;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;
import static com.soundcloud.android.offline.OfflineState.REQUESTED;
import static com.soundcloud.android.offline.OfflineState.UNAVAILABLE;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.trackItemWithOfflineState;
import static com.soundcloud.java.collections.Lists.transform;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.LikesStorage;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import edu.emory.mathcs.backport.java.util.Collections;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class OfflineStateOperationsTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);

    private OfflineStateOperations operations;

    @Mock private IsOfflineLikedTracksEnabledCommand isOfflineLikedEnabledCommand;
    @Mock private PlaylistStorage playlistStorage;
    @Mock private LikesStorage likesStorage;
    @Mock private TrackItemRepository trackRepository;
    @Mock private OfflineContentStorage offlineContentStorage;
    @Mock private TrackDownloadsStorage trackDownloadsStorage;
    @Mock private LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand;

    @Before
    public void setUp() throws Exception {
        operations = new OfflineStateOperations(isOfflineLikedEnabledCommand,
                                                likesStorage,
                                                trackRepository,
                                                offlineContentStorage,
                                                trackDownloadsStorage,
                                                playlistStorage,
                                                Schedulers.trampoline(),
                                                loadOfflinePlaylistsCommand);

        when(isOfflineLikedEnabledCommand.toSingle(null)).thenReturn(Single.just(true));
    }

    @Test
    public void returnsEmptyWhenTheTrackIsNotRelatedToAPlaylist() {
        when(isOfflineLikedEnabledCommand.toSingle(null)).thenReturn(Single.just(false));
        final List<Urn> input = singletonList(TRACK1);

        when(playlistStorage.loadPlaylistsWithTracks(input)).thenReturn(Single.just(emptySet()));
        when(loadOfflinePlaylistsCommand.toSingle()).thenReturn(Single.just(emptyList()));
        when(likesStorage.loadTrackLikes()).thenReturn(Single.just(emptyList()));


        final Single<Map<OfflineState, TrackCollections>> collections = operations.loadTracksCollectionsState(input,
                                                                                                              DOWNLOADING);
        assertThat(collections.test().values().get(0)).isEmpty();
    }

    @Test
    public void returnRequestedWhenATrackNewStateIsDownloading() {
        setPlaylist(TRACK1, PLAYLIST, trackItemWithOfflineState(TRACK1, REQUESTED));

        final Single<Map<OfflineState, TrackCollections>> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      DOWNLOADING);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections.test().values().get(0)).containsExactly(entry(DOWNLOADING, expected));
    }

    @Test
    public void returnRequestedWhenATrackNewStateIsRequested() {
        setPlaylist(TRACK1, PLAYLIST, trackItemWithOfflineState(TRACK1, REQUESTED), trackItemWithOfflineState(TRACK2, DOWNLOADED));

        final Single<Map<OfflineState, TrackCollections>> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      REQUESTED);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections.test().values().get(0)).containsExactly(entry(REQUESTED, expected));
    }

    @Test
    public void returnRequestedWhenAtLeastOneTrackIsRequested() {
        setPlaylist(TRACK1, PLAYLIST, trackItemWithOfflineState(TRACK1, REQUESTED), trackItemWithOfflineState(TRACK2, REQUESTED));

        final Single<Map<OfflineState, TrackCollections>> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      DOWNLOADED);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections.test().values().get(0)).containsExactly(entry(REQUESTED, expected));
    }

    @Test
    public void returnUnavailable() {
        setPlaylist(TRACK1, PLAYLIST, trackItemWithOfflineState(TRACK1, NOT_OFFLINE), trackItemWithOfflineState(TRACK2, UNAVAILABLE));

        final Single<Map<OfflineState, TrackCollections>> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      NOT_OFFLINE);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections.test().values().get(0)).containsExactly(entry(UNAVAILABLE, expected));
    }

    @Test
    public void returnDownloaded() {
        setPlaylist(TRACK1, PLAYLIST, trackItemWithOfflineState(TRACK1, UNAVAILABLE), trackItemWithOfflineState(TRACK2, DOWNLOADED));

        final Single<Map<OfflineState, TrackCollections>> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      UNAVAILABLE);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections.test().values().get(0)).containsExactly(entry(DOWNLOADED, expected));
    }

    @Test
    public void getLikedTracksOfflineStateReturnsNoOfflineWhenOfflineLikedTrackAreDisabled() {
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Single.just(false));

        operations.loadLikedTracksOfflineState().test()
                  .assertValue(NOT_OFFLINE);
    }

    @Test
    public void getLikedTracksOfflineStateReturnsStateFromStorageWhenOfflineLikedTracksAreEnabled() {
        List<TrackItem> tracksList = Arrays.asList(
                trackItemWithOfflineState(TRACK1, OfflineState.REQUESTED),
                trackItemWithOfflineState(TRACK2, OfflineState.UNAVAILABLE));

        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Single.just(true));
        when(likesStorage.loadTrackLikes()).thenReturn(Single.just(transform(tracksList, entity -> new Association(entity.getUrn(), new Date()))));
        when(trackDownloadsStorage.getOfflineStates(transform(tracksList, TrackItem::getUrn))).thenReturn(toMap(tracksList));

        operations.loadLikedTracksOfflineState().test().assertValue(REQUESTED);
    }

    private void setPlaylist(Urn track, Urn playlist, TrackItem... tracks) {
        final List<TrackItem> tracksList = Arrays.asList(tracks);
        when(playlistStorage.loadPlaylistsWithTracks(singletonList(track))).thenReturn(Single.just(Collections.singleton(playlist)));
        when(loadOfflinePlaylistsCommand.toSingle()).thenReturn(Single.just(singletonList(playlist)));
        when(trackRepository.forPlaylist(playlist)).thenReturn(Single.just(tracksList));
        when(likesStorage.loadTrackLikes()).thenReturn(Single.just(transform(tracksList, entity -> new Association(entity.getUrn(), new Date()))));
        when(trackDownloadsStorage.getOfflineStates(transform(tracksList, TrackItem::getUrn))).thenReturn(toMap(tracksList));
    }

    private Single<Map<Urn, OfflineState>> toMap(List<TrackItem> tracksList) {
        HashMap<Urn,OfflineState> map = new HashMap<>();
        for (TrackItem trackItem : tracksList) {
            map.put(trackItem.getUrn(), trackItem.offlineState());
        }
        return Single.just(map);
    }

}
