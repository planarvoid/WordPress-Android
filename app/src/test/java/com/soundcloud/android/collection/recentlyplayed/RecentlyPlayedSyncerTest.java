package com.soundcloud.android.collection.recentlyplayed;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.stations.StationsRepository;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchUsersCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecentlyPlayedSyncerTest extends AndroidUnitTest {

    @Mock private RecentlyPlayedStorage recentlyPlayedStorage;
    @Mock private FetchRecentlyPlayedCommand fetchRecentlyPlayedCommand;
    @Mock private PushRecentlyPlayedCommand pushRecentlyPlayedCommand;
    @Mock private RecentlyPlayedSyncer syncer;
    @Mock private FetchPlaylistsCommand fetchPlaylistsCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private FetchUsersCommand fetchUsersCommand;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private EventBus eventBus;
    @Mock private StationsOperations stationsOperations;
    @Mock private StationsRepository stationsRepository;

    @Before
    public void setUp() throws Exception {
        when(recentlyPlayedStorage.loadSyncedRecentlyPlayed()).thenReturn(Collections.emptyList());
        when(stationsOperations.station(any(Urn.class))).thenReturn(Maybe.empty());
        when(stationsRepository.syncStationsMetadata(any())).thenReturn(Completable.complete());

        syncer = new RecentlyPlayedSyncer(recentlyPlayedStorage, fetchRecentlyPlayedCommand, pushRecentlyPlayedCommand,
                                          fetchPlaylistsCommand, storePlaylistsCommand, fetchUsersCommand,
                                          storeUsersCommand, eventBus, stationsRepository);
    }

    @Test
    public void shouldSyncExistingRecentlyPlayed() throws Exception {
        PlayHistoryRecord existing = PlayHistoryRecord.create(1000L, Urn.NOT_SET, Urn.forPlaylist(1L));
        PlayHistoryRecord missing = contextFor(Urn.forPlaylist(2L));
        PlayHistoryRecord removed = PlayHistoryRecord.create(3000L, Urn.NOT_SET, Urn.forPlaylist(3L));
        when(recentlyPlayedStorage.loadSyncedRecentlyPlayed()).thenReturn(Arrays.asList(existing, removed));
        when(fetchRecentlyPlayedCommand.call()).thenReturn(Arrays.asList(existing, missing));

        syncer.call();

        verify(recentlyPlayedStorage).insertRecentlyPlayed(singletonList(missing));
        verify(recentlyPlayedStorage).removeRecentlyPlayed(singletonList(removed));
    }

    @Test
    public void shouldPreloadNewPlaylists() throws Exception {
        Urn playlistUrn = Urn.forPlaylist(123L);
        List<ApiPlaylist> playlists = singletonList(PlaylistFixtures.apiPlaylist());
        when(recentlyPlayedStorage.loadSyncedRecentlyPlayed()).thenReturn(Collections.emptyList());
        when(fetchRecentlyPlayedCommand.call()).thenReturn(singletonList(contextFor(playlistUrn)));
        when(fetchPlaylistsCommand.call()).thenReturn(playlists);

        syncer.call();

        verify(storePlaylistsCommand).call(playlists);
    }

    @Test
    public void shouldPreloadNewArtists() throws Exception {
        Urn userUrn = Urn.forUser(123L);
        List<ApiUser> users = singletonList(UserFixtures.apiUser());
        when(fetchRecentlyPlayedCommand.call()).thenReturn(singletonList(contextFor(userUrn)));
        when(fetchUsersCommand.with(singletonList(userUrn)).call()).thenReturn(users);

        syncer.call();

        verify(storeUsersCommand).call(users);
    }

    @Test
    public void shouldPreloadNewStations() throws Exception {
        Urn stationUrn = Urn.forTrackStation(123L);
        when(fetchRecentlyPlayedCommand.call()).thenReturn(singletonList(contextFor(stationUrn)));

        syncer.call();

        verify(stationsRepository).syncStationsMetadata(Collections.singletonList(stationUrn));
    }

    @Test
    public void shouldPushUnSyncedRecentlyPlayed() throws Exception {
        syncer.call();

        verify(pushRecentlyPlayedCommand).call();
    }

    @Test
    public void shouldOptimizeRecentlyPlayed() throws Exception {
        syncer.call();

        verify(recentlyPlayedStorage).trim(1000);
    }

    private PlayHistoryRecord contextFor(Urn urn) {
        return PlayHistoryRecord.create(2000L, Urn.NOT_SET, urn);
    }

}
