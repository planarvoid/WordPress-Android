package com.soundcloud.android.stations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class StationsRepositoryTest {

    @Mock private StationsStorage stationsStorage;
    @Mock private StationsApi stationsApi;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StoreStationCommand storeStationCommand;
    @Mock private SyncStateStorage syncStateStorage;
    @Mock private SyncInitiator syncInitiator;

    private final Urn station = Urn.forTrackStation(123L);
    private final Urn track = Urn.forTrack(123L);
    private StationsRepository stationsRepository;
    private StationRecord stationFromDisk;
    private ApiStation apiStation;

    private Urn firstStationUrn = Urn.forArtistStation(1);
    private StationMetadata firstStationMetadata = StationMetadata.builder()
                                                                  .urn(firstStationUrn)
                                                                  .title("")
                                                                  .type("")
                                                                  .imageUrlTemplate(Optional.absent())
                                                                  .permalink(Optional.absent())
                                                                  .build();

    private Urn secondStationUrn = Urn.forArtistStation(2);
    private StationMetadata secondStationMetadata = StationMetadata.builder()
                                                                   .urn(secondStationUrn)
                                                                   .title("")
                                                                   .type("")
                                                                   .imageUrlTemplate(Optional.absent())
                                                                   .permalink(Optional.absent())
                                                                   .build();

    @Before
    public void setUp() throws Exception {
        stationFromDisk = StationFixtures.getStation(station);
        apiStation = StationFixtures.getApiStation();

        when(stationsStorage.station(station)).thenReturn(Maybe.just(stationFromDisk));
        when(stationsApi.fetchStation(station)).thenReturn(Single.just(apiStation));

        stationsRepository = new StationsRepository(stationsStorage,
                                                    Schedulers.trampoline(),
                                                    stationsApi,
                                                    storeTracksCommand,
                                                    storeStationCommand,
                                                    syncStateStorage,
                                                    syncInitiator);
    }

    @Test
    public void getStationShouldReturnAStationFromDiskIfDataIsAvailableInDatabase() {
        stationsRepository.station(station)
                          .test()
                          .assertValue(stationFromDisk);
    }

    @Test
    public void getStationShouldFallBackToNetworkWhenTracksMissing() {
        when(stationsStorage.station(station)).thenReturn(Maybe.empty());

        stationsRepository.station(station)
                          .test()
                          .assertValue(apiStation)
                          .assertNever(stationFromDisk);
    }

    @Test
    public void shouldPersistApiStation() throws Exception {

        when(stationsApi.fetchStation(station)).thenReturn(Single.just(apiStation));

        stationsRepository.syncSingleStation(station, StationsRepository.IDENTITY).test();

        verify(storeTracksCommand).call(apiStation.getTrackRecords());
        verify(storeStationCommand).call(eq(apiStation));
    }

    @Test
    public void fetchUpcomingTracksShouldFetchAndReturnTrackFromStorage() {
        final ApiStation apiStationWithTracks = StationFixtures.getApiStation(station, 10);
        final List<StationTrack> tracks = apiStationWithTracks.getTracks();
        final List<StationTrack> subTrackList = tracks.subList(2, tracks.size());

        when(stationsApi.fetchStation(station)).thenReturn(Single.just(apiStationWithTracks));
        when(stationsStorage.loadStationPlayQueue(station, 2)).thenReturn(Single.just(subTrackList));

        stationsRepository.loadStationPlayQueue(station, 2).test();

        verify(stationsApi).fetchStation(station);
        verify(stationsStorage).loadStationPlayQueue(station, 2);
    }

    @Test
    public void shouldClearExpiredPlayQueue() {
        stationsRepository.clearExpiredPlayQueue(station);

        verify(stationsStorage).clearExpiredPlayQueue(station);
    }

    @Test
    public void loadCollectionShouldReturnAStationFromDiskIfDataIsSynced() {
        when(stationsStorage.getStationsCollection(anyInt())).thenReturn(Single.just(Collections.singletonList(stationFromDisk)));
        when(syncStateStorage.hasSyncedBefore(Syncable.LIKED_STATIONS)).thenReturn(true);

        stationsRepository.collection(StationsCollectionsTypes.LIKED).test();

        verify(stationsStorage).getStationsCollection(StationsCollectionsTypes.LIKED);
        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void loadCollectionShouldSyncAndReturnAStationFromDiskIfDataIsNotSynced() {
        when(stationsStorage.getStationsCollection(anyInt())).thenReturn(Single.just(Collections.singletonList(stationFromDisk)));
        when(syncStateStorage.hasSyncedBefore(Syncable.LIKED_STATIONS)).thenReturn(false);
        when(syncInitiator.sync(any(Syncable.class))).thenReturn(Single.just(SyncJobResult.success("", true)));

        stationsRepository.collection(StationsCollectionsTypes.LIKED).test();

        verify(syncInitiator).sync(Syncable.LIKED_STATIONS);
        verify(stationsStorage).getStationsCollection(StationsCollectionsTypes.LIKED);
    }

    @Test
    public void loadStationWithTracksShouldSyncIfTracksAreMissing() {

        final StationWithTrackUrns stationWithoutTracks = StationWithTrackUrns.builder()
                                                                              .urn(Urn.NOT_SET)
                                                                              .title("")
                                                                              .type("")
                                                                              .permalink(Optional.absent())
                                                                              .imageUrlTemplate(Optional.absent())
                                                                              .lastPlayedTrackPosition(0)
                                                                              .liked(true)
                                                                              .build();

        final StationWithTrackUrns stationWithTrack = stationWithoutTracks.toBuilder()
                                                                          .trackUrns(Collections.singletonList(track))
                                                                          .build();

        when(stationsStorage.stationWithTrackUrns(any())).thenReturn(Maybe.just(stationWithoutTracks),
                                                                     Maybe.just(stationWithTrack));

        stationsRepository.stationWithTrackUrns(station, StationsRepository.IDENTITY).test();

        verify(stationsApi).fetchStation(station);
        verify(stationsStorage, times(2)).stationWithTrackUrns(station);
    }

    @Test
    public void loadStationWithTracksShouldNotSyncIfThereAreTracks() {

        final StationWithTrackUrns stationWithTracks = StationWithTrackUrns.builder()
                                                                           .urn(Urn.NOT_SET)
                                                                           .title("")
                                                                           .type("")
                                                                           .permalink(Optional.absent())
                                                                           .imageUrlTemplate(Optional.absent())
                                                                           .lastPlayedTrackPosition(0)
                                                                           .liked(true)
                                                                           .trackUrns(Collections.singletonList(track))
                                                                           .build();

        when(stationsStorage.stationWithTrackUrns(any())).thenReturn(Maybe.just(stationWithTracks));

        stationsRepository.stationWithTrackUrns(station, StationsRepository.IDENTITY).test();

        verify(stationsStorage, times(1)).stationWithTrackUrns(station);
    }

    @Test
    public void shouldClearStorageOnClearData() {
        stationsRepository.clearData();

        verify(stationsStorage).clear();
    }

    @Test
    public void loadStationsMetadataShouldSyncMissingTrack() throws ApiRequestException, IOException, ApiMapperException {
        when(stationsStorage.loadStationsMetadata(any())).thenReturn(Single.just(Collections.singletonList(firstStationMetadata)),
                                                                     Single.just(Arrays.asList(firstStationMetadata, secondStationMetadata)));
        when(stationsApi.fetchStations(any())).thenReturn(Collections.emptyList());

        stationsRepository.stationsMetadata(Arrays.asList(firstStationUrn, secondStationUrn))
                          .test()
                          .assertValue(Arrays.asList(firstStationMetadata, secondStationMetadata));

        verify(stationsApi).fetchStations(Collections.singletonList(secondStationUrn));
    }

    @Test
    public void loadStationsMetadataShouldIgnoreSyncErrors() throws ApiRequestException, IOException, ApiMapperException {
        when(stationsStorage.loadStationsMetadata(any())).thenReturn(Single.just(Collections.singletonList(firstStationMetadata)));
        when(stationsApi.fetchStations(any())).thenThrow(new RuntimeException());

        stationsRepository.stationsMetadata(Arrays.asList(firstStationUrn, secondStationUrn))
                          .test()
                          .assertNoErrors()
                          .assertValue(Collections.singletonList(firstStationMetadata));
    }
}
