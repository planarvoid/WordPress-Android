package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
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
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;

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
    private TestEventBus eventBus;
    private StationsOperations operations;
    private StationRecord stationFromDisk;
    private ApiStation apiStation;

    @Before
    public void setUp() {
        stationFromDisk = StationFixtures.getStation(Urn.forTrackStation(123L));
        apiStation = StationFixtures.getApiStation();
        eventBus = new TestEventBus();

        operations = new StationsOperations(
                syncStateStorage,
                stationsStorage,
                stationsApi,
                storeTracksCommand,
                storeStationCommand,
                syncInitiator,
                Schedulers.immediate(),
                eventBus,
                trackRepository);

        when(stationsStorage.clearExpiredPlayQueue(station)).thenReturn(Observable.just(new TxnResult()));
        when(stationsStorage.station(station)).thenReturn(Observable.just(stationFromDisk));
        when(stationsApi.fetchStation(station)).thenReturn(Observable.just(apiStation));
    }

    @Test
    public void getStationShouldReturnAStationFromDiskIfDataIsAvailableInDatabase() {
        final TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();
        operations.station(station).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Collections.singletonList(stationFromDisk));
    }

    @Test
    public void getStationShouldFallbackToNetwork() {
        when(stationsStorage.station(station)).thenReturn(Observable.empty());

        final TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();
        operations.station(station).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).doesNotContain(stationFromDisk);
        subscriber.assertCompleted();
    }

    @Test
    public void getStationShouldFallBackToNetworkWhenTracksMissing() {
        StationRecord noTracksStation = StationFixtures.getApiStation(Urn.forTrackStation(123), 0);
        when(stationsStorage.station(station)).thenReturn(Observable.just(noTracksStation));

        final TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();
        operations.station(station).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).doesNotContain(stationFromDisk);
        subscriber.assertCompleted();
    }

    @Test
    public void fetchUpcomingTracksShouldFetchAndReturnTrackFromStorage() {
        final TestSubscriber<PlayQueue> subscriber = new TestSubscriber<>();
        final Urn station = Urn.forTrackStation(123L);
        final ApiStation stationApi = StationFixtures.getApiStation(Urn.forTrackStation(123L), 10);
        final List<StationTrack> tracks = stationApi.getTracks();
        final List<StationTrack> subTrackList = tracks.subList(2, tracks.size());
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.STATIONS_INFO, station);

        when(stationsApi.fetchStation(station)).thenReturn(Observable.just(stationApi));
        when(stationsStorage.loadPlayQueue(station, 2)).thenReturn(Observable.from(subTrackList));

        operations.fetchUpcomingTracks(station, 2, playSessionSource).subscribe(subscriber);

        subscriber.assertValue(PlayQueue.fromStation(station, subTrackList, playSessionSource));
    }


    @Test
    public void fetchUpcomingTracksShouldHaveASuggestionsSource() {
        final TestSubscriber<PlayQueue> subscriber = new TestSubscriber<>();
        final Urn station = Urn.forTrackStation(123L);
        final ApiStation stationApi = StationFixtures.getApiStation(Urn.forTrackStation(123L), 10);
        final List<StationTrack> tracks = stationApi.getTracks();
        final List<StationTrack> subTrackList = tracks.subList(2, tracks.size());
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.STATIONS_INFO, station);

        when(stationsApi.fetchStation(station)).thenReturn(Observable.just(stationApi));
        when(stationsStorage.loadPlayQueue(station, 2)).thenReturn(Observable.from(subTrackList));

        operations.fetchUpcomingTracks(station, 2, playSessionSource).subscribe(subscriber);

        final TrackQueueItem playQueueItem = (TrackQueueItem) subscriber.getOnNextEvents()
                                                                        .get(0)
                                                                        .getPlayQueueItem(0);
        assertThat(playQueueItem.getSource()).isEqualTo("stations:suggestions");
    }

    @Test
    public void shouldPersistApiStation() {
        when(stationsStorage.station(station)).thenReturn(Observable.empty());

        final TestSubscriber<Object> subscriber = new TestSubscriber<>();
        operations.station(station).subscribe(subscriber);

        verify(storeTracksCommand).call(apiStation.getTrackRecords());
        verify(storeStationCommand).call(eq(apiStation));
    }

    @Test
    public void shouldPersistNewLikedStation() {
        when(stationsStorage.updateLocalStationLike(any(Urn.class),
                                                    anyBoolean())).thenReturn(Observable.just(mock(ChangeResult.class)));

        operations.toggleStationLike(station, true).subscribe();

        verify(stationsStorage).updateLocalStationLike(station, true);
    }

    @Test
    public void shouldEmitStationUpdatedEventWhenLikingAStation() {
        when(stationsStorage.updateLocalStationLike(any(Urn.class),
                                                    anyBoolean())).thenReturn(Observable.just(mock(ChangeResult.class)));

        operations.toggleStationLike(station, true).subscribe();

        final UrnStateChangedEvent event = eventBus.lastEventOn(EventQueue.URN_STATE_CHANGED,
                                                                   UrnStateChangedEvent.class);
        assertThat(event.kind()).isEqualTo(UrnStateChangedEvent.Kind.STATIONS_COLLECTION_UPDATED);
        assertThat(event.urns().iterator().next()).isEqualTo(station);
    }

    @Test
    public void shouldReturnOnboardingStreamItemWhenPreferenceIsEnabled() {
        when(stationsStorage.isOnboardingStreamItemDisabled()).thenReturn(false);
        final TestSubscriber<StreamItem> subscriber = new TestSubscriber<>();
        operations.onboardingStreamItem().subscribe(subscriber);
        subscriber.assertValueCount(1);
        assertThat(subscriber.getOnNextEvents().get(0).kind()).isEqualTo(StreamItem.Kind.STATIONS_ONBOARDING);
    }

    @Test
    public void shouldNotReturnOnboardingStreamItemWhenPreferenceIsDisabled() {
        when(stationsStorage.isOnboardingStreamItemDisabled()).thenReturn(true);
        final TestSubscriber<StreamItem> subscriber = new TestSubscriber<>();
        operations.onboardingStreamItem().subscribe(subscriber);
        subscriber.assertNoValues();
    }
}
