package com.soundcloud.android.discovery.systemplaylist;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.discovery.DiscoveryReadableStorage;
import com.soundcloud.android.discovery.DiscoveryWritableStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SystemPlaylistOperationsTest extends AndroidUnitTest {
    private static final Urn URN = Urn.forSystemPlaylist("123");
    private static final Urn TRACK_URN = Urn.forTrack(456L);


    @Mock private ApiClientRxV2 apiClient;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private DiscoveryWritableStorage discoveryWriteableStorage;
    @Mock private DiscoveryReadableStorage discoveryReadableStorage;
    @Mock private TrackRepository trackRepository;

    private SystemPlaylistOperations operations;

    private List<Urn> trackUrns = Collections.singletonList(TRACK_URN);

    private final SystemPlaylistEntity systemPlaylistEntity = SystemPlaylistEntity.create(
            URN,
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            trackUrns,
            Optional.absent(),
            Optional.absent(),
            Optional.absent()
    );

    private final List<Track> tracks = Collections.singletonList(ModelFixtures.trackBuilder().urn(TRACK_URN).build());

    private final SystemPlaylist systemPlaylist = SystemPlaylistMapper.map(systemPlaylistEntity, tracks);

    private final SystemPlaylist systemPlaylistWithNoTracks = SystemPlaylistMapper.map(systemPlaylistEntity, new ArrayList<>());

    @Before
    public void setUp() throws Exception {
        apiReturns(Single.never());

        this.operations = new SystemPlaylistOperations(apiClient, storeTracksCommand, Schedulers.trampoline(), discoveryWriteableStorage, discoveryReadableStorage, trackRepository);
    }

    @Test
    public void systemPlaylist_returnsSystemPlaylistFromStorageWhenAllDataIsStored() {
        initializeStorage(Optional.of(systemPlaylistEntity), tracks);
        apiReturns(Single.error(new IOException()));

        operations.systemPlaylist(URN).test()
                  .assertValueCount(1)
                  .assertValue(systemPlaylist)
                  .assertNoErrors();

        verify(storeTracksCommand, never()).call(any());
        verify(discoveryWriteableStorage, never()).storeSystemPlaylist(any());
    }

    @Test
    public void systemPlaylist_callsApiAndStoresDataIfFetchingSystemPlaylistReturnsEmpty() {
        ApiSystemPlaylist apiSystemPlaylist = ModelFixtures.apiSystemPlaylist();
        SystemPlaylist mappedSystemPlaylist = SystemPlaylistMapper.map(apiSystemPlaylist);
        initializeStorage(Optional.absent(), tracks);
        apiReturns(Single.just(apiSystemPlaylist));

        operations.systemPlaylist(URN).test()
                  .assertValueCount(1)
                  .assertValue(mappedSystemPlaylist)
                  .assertNoErrors();

        verify(storeTracksCommand).call(apiSystemPlaylist.tracks());
        verify(discoveryWriteableStorage).storeSystemPlaylist(apiSystemPlaylist);
    }

    @Test
    public void systemPlaylist_doesNotCallApiOrStoreDataIfFetchingTracksFromUrnsReturnsEmptyList() {
        initializeStorage(Optional.of(systemPlaylistEntity), Lists.newArrayList());

        operations.systemPlaylist(URN).test()
                  .assertValueCount(1)
                  .assertValue(systemPlaylistWithNoTracks)
                  .assertNoErrors();

        verify(storeTracksCommand, never()).call(anyIterable());
        verify(discoveryWriteableStorage, never()).storeSystemPlaylist(any(ApiSystemPlaylist.class));
    }

    @Test
    public void systemPlaylist_putsUrnInApiRequestUrl() throws Exception {
        initializeStorage(Optional.absent(), Lists.newArrayList());

        operations.systemPlaylist(URN).test();

        ArgumentCaptor<ApiRequest> requestCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClient).mappedResponse(requestCaptor.capture(), eq(ApiSystemPlaylist.class));

        assertThat(requestCaptor.getValue().getUri().toString()).contains(ApiEndpoints.SYSTEM_PLAYLISTS.path(URN));
    }

    @Test
    public void systemPlaylist_failsOnStorageAndApiFailure() throws Exception {
        initializeStorage(Optional.absent(), Lists.newArrayList());
        apiReturns(Single.error(new IOException()));

        operations.systemPlaylist(URN).test()
                  .assertNoValues()
                  .assertError(IOException.class);

        verify(storeTracksCommand, never()).call(anyIterable());
        verify(discoveryWriteableStorage, never()).storeSystemPlaylist(any(ApiSystemPlaylist.class));
    }

    @Test
    public void refreshSystemPlaylist_callsApiAndStoresData() {
        ApiSystemPlaylist apiSystemPlaylist = ModelFixtures.apiSystemPlaylist();
        SystemPlaylist mappedSystemPlaylist = SystemPlaylistMapper.map(apiSystemPlaylist);
        apiReturns(Single.just(apiSystemPlaylist));

        operations.refreshSystemPlaylist(URN).test()
                  .assertValueCount(1)
                  .assertValue(mappedSystemPlaylist)
                  .assertNoErrors();

        verify(storeTracksCommand).call(apiSystemPlaylist.tracks());
        verify(discoveryWriteableStorage).storeSystemPlaylist(apiSystemPlaylist);
    }

    @Test
    public void refreshSystemPlaylist_putsUrnInApiRequestUrl() throws Exception {
        operations.refreshSystemPlaylist(URN).test();

        ArgumentCaptor<ApiRequest> requestCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClient).mappedResponse(requestCaptor.capture(), eq(ApiSystemPlaylist.class));

        assertThat(requestCaptor.getValue().getUri().toString()).contains(ApiEndpoints.SYSTEM_PLAYLISTS.path(URN));
    }

    @Test
    public void refreshSystemPlaylist_failsOnApiFailure() throws Exception {
        apiReturns(Single.error(new IOException()));

        operations.refreshSystemPlaylist(URN).test()
                  .assertNoValues()
                  .assertError(IOException.class);

        verify(storeTracksCommand, never()).call(anyIterable());
        verify(discoveryWriteableStorage, never()).storeSystemPlaylist(any(ApiSystemPlaylist.class));
    }

    private void initializeStorage(Optional<SystemPlaylistEntity> systemPlaylists, List<Track> tracks) {
        when(discoveryReadableStorage.systemPlaylistEntity(URN)).thenReturn(systemPlaylists.isPresent() ? Maybe.just(systemPlaylists.get()) : Maybe.empty());
        when(trackRepository.trackListFromUrns(trackUrns)).thenReturn(Single.just(tracks));
    }

    private void apiReturns(Single<ApiSystemPlaylist> response) {
        when(apiClient.mappedResponse(any(ApiRequest.class), eq(ApiSystemPlaylist.class))).thenReturn(response);
    }
}
