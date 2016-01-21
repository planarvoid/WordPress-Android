package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineState.DOWNLOADED;
import static com.soundcloud.android.offline.OfflineState.DOWNLOADING;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;
import static com.soundcloud.android.offline.OfflineState.REQUESTED;
import static com.soundcloud.android.offline.OfflineState.UNAVAILABLE;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.likes.LoadLikedTracksCommand;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTracksCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OfflineStateOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);

    private OfflineStateOperations operations;

    @Mock private IsOfflineLikedTracksEnabledCommand isOfflineLikedEnabledCommand;
    @Mock private LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand;
    @Mock private LoadLikedTrackUrnsCommand loadLikedTracksUrnsCommand;
    @Mock private LoadPlaylistTracksCommand loadPlaylistsTracksCommand;
    @Mock private LoadLikedTracksCommand loadLikedTracksCommand;
    @Mock private OfflineContentStorage offlineContentStorage;
    @Mock private TrackDownloadsStorage trackDownloadsStorage;

    @Before
    public void setUp() throws Exception {
        operations = new OfflineStateOperations(
                isOfflineLikedEnabledCommand,
                loadOfflinePlaylistsCommand,
                loadLikedTracksUrnsCommand,
                loadPlaylistsTracksCommand,
                loadLikedTracksCommand,
                offlineContentStorage,
                trackDownloadsStorage,
                Schedulers.immediate());

        when(isOfflineLikedEnabledCommand.call(null)).thenReturn(true);
    }

    @Test
    public void returnsEmptyWhenTheTrackIsNotRelatedToAPlaylist() {
        when(isOfflineLikedEnabledCommand.call(null)).thenReturn(false);
        when(loadOfflinePlaylistsCommand.call(TRACK1)).thenReturn(Collections.<Urn>emptyList());


        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(TRACK1, DOWNLOADING);

        assertThat(collections).isEmpty();
    }

    @Test
    public void returnRequestedWhenATrackNewStateIsDownloading() {
        setPlaylist(TRACK1, PLAYLIST, createTrack(TRACK1, REQUESTED));

        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(TRACK1, DOWNLOADING);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections).containsExactly(entry(DOWNLOADING, expected));
    }

    @Test
    public void returnRequestedWhenATrackNewStateIsRequested() {
        setPlaylist(TRACK1, PLAYLIST, createTrack(TRACK1, REQUESTED), createTrack(TRACK2, DOWNLOADED));

        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(TRACK1, REQUESTED);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections).containsExactly(entry(REQUESTED, expected));
    }

    @Test
    public void returnRequestedWhenAtLeastOneTrackIsRequested() {
        setPlaylist(TRACK1, PLAYLIST, createTrack(TRACK1, REQUESTED), createTrack(TRACK2, REQUESTED));

        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(TRACK1, DOWNLOADED);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections).containsExactly(entry(REQUESTED, expected));
    }

    @Test
    public void returnUnavailable() {
        setPlaylist(TRACK1, PLAYLIST, createTrack(TRACK1, NOT_OFFLINE), createTrack(TRACK2, UNAVAILABLE));

        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(TRACK1, NOT_OFFLINE);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections).containsExactly(entry(UNAVAILABLE, expected));
    }

    @Test
    public void returnDownloaded() {
        setPlaylist(TRACK1, PLAYLIST, createTrack(TRACK1, UNAVAILABLE), createTrack(TRACK2, DOWNLOADED));

        final Map<OfflineState, TrackCollections> collections = operations.loadTracksCollectionsState(TRACK1, UNAVAILABLE);

        final TrackCollections expected = TrackCollections.create(singletonList(PLAYLIST), true);
        assertThat(collections).containsExactly(entry(DOWNLOADED, expected));
    }

    @Test
    public void getLikedTracksOfflineStateReturnsNoOfflineWhenOfflineLikedTrackAreDisabled() {
        final TestSubscriber<OfflineState> subscriber = new TestSubscriber<>();
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(false));

        operations.loadLikedTracksOfflineState().subscribe(subscriber);

        subscriber.assertValue(OfflineState.NOT_OFFLINE);
    }

    @Test
    public void getLikedTracksOfflineStateReturnsStateFromStorageWhenOfflineLikedTracksAreEnabled() {
        final TestSubscriber<OfflineState> subscriber = new TestSubscriber<>();
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(true));
        when(trackDownloadsStorage.getLikesOfflineState()).thenReturn(Observable.just(OfflineState.REQUESTED));

        operations.loadLikedTracksOfflineState().subscribe(subscriber);

        subscriber.assertValue(OfflineState.REQUESTED);
    }

    private void setPlaylist(Urn track, Urn playlist, PropertySet... tracks) {
        final List<PropertySet> tracksList = Arrays.asList(tracks);
        when(loadOfflinePlaylistsCommand.call(track)).thenReturn(singletonList(playlist));
        when(loadPlaylistsTracksCommand.call(playlist)).thenReturn(tracksList);
        when(loadLikedTracksUrnsCommand.call(null)).thenReturn(Lists.transform(tracksList, new Function<PropertySet, Urn>() {
            @Override
            public Urn apply(PropertySet entity) {
                return entity.get(EntityProperty.URN);
            }
        }));
        when(loadLikedTracksCommand.call(null)).thenReturn(tracksList);
    }

    private PropertySet createTrack(Urn track, OfflineState state) {
        return PropertySet.from(
                TrackProperty.URN.bind(track),
                OfflineProperty.OFFLINE_STATE.bind(state)
        );
    }

}
