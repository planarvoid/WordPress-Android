package com.soundcloud.android.stations;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class StationsOperationsTest {

    @Mock StationsRepository stationsRepository;
    @Mock TrackItemRepository trackRepository;

    private final Urn station = Urn.forTrackStation(123L);
    private TestEventBusV2 eventBus;
    private StationsOperations operations;
    private StationRecord stationFromDisk;

    @Before
    public void setUp() {
        stationFromDisk = StationFixtures.getStation(Urn.forTrackStation(123L));
        eventBus = new TestEventBusV2();

        operations = new StationsOperations(
                stationsRepository,
                Schedulers.trampoline(),
                eventBus,
                trackRepository);

        when(stationsRepository.clearExpiredPlayQueue(station)).thenReturn(Completable.complete());
        when(stationsRepository.station(station)).thenReturn(Maybe.just(stationFromDisk));
    }

    @Test
    public void getStationShouldReturnAStationFromDiskIfDataIsAvailableInDatabase() {
        operations.station(station).test().assertValue(stationFromDisk);
    }

    @Test
    public void getStationShouldFallbackToNetwork() {
        when(stationsRepository.station(station)).thenReturn(Maybe.empty());

        operations.station(station)
                  .test()
                  .assertNoValues();
    }

    @Test
    public void getStationShouldFallBackToNetworkWhenTracksMissing() {
        StationRecord noTracksStation = StationFixtures.getApiStation(Urn.forTrackStation(123), 0);
        when(stationsRepository.station(station)).thenReturn(Maybe.just(noTracksStation));

        operations.station(station)
                  .test()
                  .assertNever(stationFromDisk)
                  .assertComplete();
    }

    @Test
    public void fetchUpcomingTracksShouldHaveASuggestionsSource() {
        final Urn station = Urn.forTrackStation(123L);
        final ApiStation stationApi = StationFixtures.getApiStation(Urn.forTrackStation(123L), 10);
        final List<StationTrack> tracks = stationApi.getTracks();
        final List<StationTrack> subTrackList = tracks.subList(2, tracks.size());
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.STATIONS_INFO, station);

        when(stationsRepository.loadStationPlayQueue(station, 2)).thenReturn(Single.just(subTrackList));

        final List<PlayQueue> values = operations.fetchUpcomingTracks(station, 2, playSessionSource)
                                                 .test()
                                                 .values();

        final TrackQueueItem playQueueItem = (TrackQueueItem) values.get(0)
                                                                    .getPlayQueueItem(0);

        assertThat(playQueueItem.getSource()).isEqualTo("stations:suggestions");
    }

    @Test
    public void shouldPersistNewLikedStation() {
        when(stationsRepository.updateLocalStationLike(any(Urn.class), anyBoolean())).thenReturn(Completable.complete());

        operations.toggleStationLike(station, true).subscribe();

        verify(stationsRepository).updateLocalStationLike(station, true);
    }

    @Test
    public void shouldEmitStationUpdatedEventWhenLikingAStation() {
        when(stationsRepository.updateLocalStationLike(any(Urn.class), anyBoolean())).thenReturn(Completable.complete());

        operations.toggleStationLike(station, true).subscribe();

        final UrnStateChangedEvent event = eventBus.lastEventOn(EventQueue.URN_STATE_CHANGED,
                                                                UrnStateChangedEvent.class);
        assertThat(event.kind()).isEqualTo(UrnStateChangedEvent.Kind.STATIONS_COLLECTION_UPDATED);
        assertThat(event.urns()).startsWith(station);
    }

}
