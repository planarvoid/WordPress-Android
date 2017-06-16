package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBusV2;

import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class StationsOperationsTest extends AndroidUnitTest {

    @Mock FeatureFlags featureFlags;
    @Mock SyncStateStorage syncStateStorage;
    @Mock StationsStorage stationsStorage;
    @Mock StationsApi stationsApi;
    @Mock StoreTracksCommand storeTracksCommand;
    @Mock StoreStationCommand storeStationCommand;
    @Mock SyncInitiator syncInitiator;
    @Mock TrackItemRepository trackRepository;

    private final Urn station = Urn.forTrackStation(123L);
    private TestEventBusV2 eventBus;
    private StationsOperations operations;
    private StationRecord stationFromDisk;
    private ApiStation apiStation;

    @Before
    public void setUp() {
        stationFromDisk = StationFixtures.getStation(Urn.forTrackStation(123L));
        apiStation = StationFixtures.getApiStation();
        eventBus = new TestEventBusV2();

        operations = new StationsOperations(
                syncStateStorage,
                stationsStorage,
                stationsApi,
                storeTracksCommand,
                storeStationCommand,
                syncInitiator,
                Schedulers.trampoline(),
                eventBus,
                trackRepository);

        when(stationsStorage.clearExpiredPlayQueue(station)).thenReturn(Single.just(new TxnResult()));
        when(stationsStorage.station(station)).thenReturn(Maybe.just(stationFromDisk));
        when(stationsApi.fetchStation(station)).thenReturn(Single.just(apiStation));
    }

    @Test
    public void getStationShouldReturnAStationFromDiskIfDataIsAvailableInDatabase() {
        operations.station(station).test().assertValue(stationFromDisk);
    }

    @Test
    public void getStationShouldFallbackToNetwork() {
        when(stationsStorage.station(station)).thenReturn(Maybe.empty());

        operations.station(station).test().assertNever(stationFromDisk).assertComplete();
    }

    @Test
    public void getStationShouldFallBackToNetworkWhenTracksMissing() {
        StationRecord noTracksStation = StationFixtures.getApiStation(Urn.forTrackStation(123), 0);
        when(stationsStorage.station(station)).thenReturn(Maybe.just(noTracksStation));

        operations.station(station).test().assertNever(stationFromDisk).assertComplete();
    }

    @Test
    public void fetchUpcomingTracksShouldFetchAndReturnTrackFromStorage() {
        final Urn station = Urn.forTrackStation(123L);
        final ApiStation stationApi = StationFixtures.getApiStation(Urn.forTrackStation(123L), 10);
        final List<StationTrack> tracks = stationApi.getTracks();
        final List<StationTrack> subTrackList = tracks.subList(2, tracks.size());
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.STATIONS_INFO, station);

        when(stationsApi.fetchStation(station)).thenReturn(Single.just(stationApi));
        when(stationsStorage.loadPlayQueue(station, 2)).thenReturn(Single.just(subTrackList));

        operations.fetchUpcomingTracks(station, 2, playSessionSource)
                  .test()
                  .assertValue(PlayQueue.fromStation(station, subTrackList, playSessionSource));
    }


    @Test
    public void fetchUpcomingTracksShouldHaveASuggestionsSource() {
        final Urn station = Urn.forTrackStation(123L);
        final ApiStation stationApi = StationFixtures.getApiStation(Urn.forTrackStation(123L), 10);
        final List<StationTrack> tracks = stationApi.getTracks();
        final List<StationTrack> subTrackList = tracks.subList(2, tracks.size());
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.STATIONS_INFO, station);

        when(stationsApi.fetchStation(station)).thenReturn(Single.just(stationApi));
        when(stationsStorage.loadPlayQueue(station, 2)).thenReturn(Single.just(subTrackList));

        final TrackQueueItem playQueueItem = (TrackQueueItem) operations.fetchUpcomingTracks(station, 2, playSessionSource)
                                                                        .test()
                                                                        .values()
                                                                        .get(0)
                                                                        .getPlayQueueItem(0);
        assertThat(playQueueItem.getSource()).isEqualTo("stations:suggestions");
    }

    @Test
    public void shouldPersistApiStation() {
        when(stationsStorage.station(station)).thenReturn(Maybe.empty());

        operations.station(station).test();

        verify(storeTracksCommand).call(apiStation.getTrackRecords());
        verify(storeStationCommand).call(apiStation);
    }

    @Test
    public void shouldPersistNewLikedStation() {
        when(stationsStorage.updateLocalStationLike(any(Urn.class),
                                                    anyBoolean())).thenReturn(Single.just(mock(ChangeResult.class)));

        operations.toggleStationLike(station, true).subscribe();

        verify(stationsStorage).updateLocalStationLike(station, true);
    }

    @Test
    public void shouldEmitStationUpdatedEventWhenLikingAStation() {
        when(stationsStorage.updateLocalStationLike(any(Urn.class),
                                                    anyBoolean())).thenReturn(Single.just(mock(ChangeResult.class)));

        operations.toggleStationLike(station, true).subscribe();

        final UrnStateChangedEvent event = eventBus.lastEventOn(EventQueue.URN_STATE_CHANGED,
                                                                   UrnStateChangedEvent.class);
        assertThat(event.kind()).isEqualTo(UrnStateChangedEvent.Kind.STATIONS_COLLECTION_UPDATED);
        assertThat(event.urns().iterator().next()).isEqualTo(station);
    }

}
