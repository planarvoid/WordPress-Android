package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineState.DOWNLOADED;
import static com.soundcloud.android.offline.OfflineState.DOWNLOADING;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;
import static com.soundcloud.android.offline.OfflineState.REQUESTED;
import static com.soundcloud.android.offline.OfflineState.UNAVAILABLE;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.trackItemWithOfflineState;
import static com.soundcloud.java.collections.Lists.transform;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.Like;
import com.soundcloud.android.likes.LoadLikedTracksCommand;
import com.soundcloud.android.likes.LoadLikedTracksOfflineStateCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class OfflineStateOperationsTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);

    private OfflineStateOperations operations;

    @Mock private IsOfflineLikedTracksEnabledCommand isOfflineLikedEnabledCommand;
    @Mock private LoadOfflinePlaylistsContainingTracksCommand loadOfflinePlaylistsContainingTracksCommand;
    @Mock private LoadLikedTracksCommand loadLikedTracksCommand;
    @Mock private TrackItemRepository trackRepository;
    @Mock private LoadLikedTracksOfflineStateCommand loadLikedTracksOfflineStateCommand;
    @Mock private OfflineContentStorage offlineContentStorage;
    @Mock private TrackDownloadsStorage trackDownloadsStorage;

    @Before
    public void setUp() throws Exception {
        operations = new OfflineStateOperations(isOfflineLikedEnabledCommand,
                                                loadOfflinePlaylistsContainingTracksCommand,
                                                loadLikedTracksCommand,
                                                trackRepository,
                                                loadLikedTracksOfflineStateCommand,
                                                offlineContentStorage,
                                                trackDownloadsStorage,
                                                Schedulers.trampoline());

        when(isOfflineLikedEnabledCommand.call(null)).thenReturn(true);
    }

    @Test
    public void returnsEmptyWhenTheTrackIsNotRelatedToAPlaylist() {
        when(isOfflineLikedEnabledCommand.call(null)).thenReturn(false);
        when(loadOfflinePlaylistsContainingTracksCommand.call(singletonList(TRACK1))).thenReturn(Collections.emptyList());


        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      DOWNLOADING);

        assertThat(collections).isEmpty();
    }

    @Test
    public void returnRequestedWhenATrackNewStateIsDownloading() {
        setPlaylist(TRACK1, PLAYLIST, trackItemWithOfflineState(TRACK1, REQUESTED));

        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      DOWNLOADING);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections).containsExactly(entry(DOWNLOADING, expected));
    }

    @Test
    public void returnRequestedWhenATrackNewStateIsRequested() {
        setPlaylist(TRACK1, PLAYLIST, trackItemWithOfflineState(TRACK1, REQUESTED), trackItemWithOfflineState(TRACK2, DOWNLOADED));

        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      REQUESTED);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections).containsExactly(entry(REQUESTED, expected));
    }

    @Test
    public void returnRequestedWhenAtLeastOneTrackIsRequested() {
        setPlaylist(TRACK1, PLAYLIST, trackItemWithOfflineState(TRACK1, REQUESTED), trackItemWithOfflineState(TRACK2, REQUESTED));

        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      DOWNLOADED);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections).containsExactly(entry(REQUESTED, expected));
    }

    @Test
    public void returnUnavailable() {
        setPlaylist(TRACK1, PLAYLIST, trackItemWithOfflineState(TRACK1, NOT_OFFLINE), trackItemWithOfflineState(TRACK2, UNAVAILABLE));

        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      NOT_OFFLINE);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections).containsExactly(entry(UNAVAILABLE, expected));
    }

    @Test
    public void returnDownloaded() {
        setPlaylist(TRACK1, PLAYLIST, trackItemWithOfflineState(TRACK1, UNAVAILABLE), trackItemWithOfflineState(TRACK2, DOWNLOADED));

        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(singletonList(TRACK1),
                                                                                                      UNAVAILABLE);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections).containsExactly(entry(DOWNLOADED, expected));
    }

    @Test
    public void getLikedTracksOfflineStateReturnsNoOfflineWhenOfflineLikedTrackAreDisabled() {
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(false));

        operations.loadLikedTracksOfflineState().test()
                  .assertValue(NOT_OFFLINE);
    }

    @Test
    public void getLikedTracksOfflineStateReturnsStateFromStorageWhenOfflineLikedTracksAreEnabled() {
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(true));
        when(trackDownloadsStorage.getLikesOfflineState()).thenReturn(rx.Observable.just(OfflineState.REQUESTED));

        operations.loadLikedTracksOfflineState().test()
                  .assertValue(REQUESTED);
    }

    private void setPlaylist(Urn track, Urn playlist, TrackItem... tracks) {
        final List<TrackItem> tracksList = Arrays.asList(tracks);
        when(loadOfflinePlaylistsContainingTracksCommand.call(singletonList(track))).thenReturn(singletonList(playlist));
        when(trackRepository.forPlaylist(playlist)).thenReturn(Single.just(tracksList));
        when(loadLikedTracksCommand.call(Optional.absent())).thenReturn(transform(tracksList, entity -> Like.create(entity.getUrn(), new Date())));
        when(loadLikedTracksOfflineStateCommand.call(null)).thenReturn(transform(tracksList, TrackItem::offlineState));
    }

}
