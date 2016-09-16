package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stream.SoundStreamItem;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

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
                eventBus);

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
    public void stationWithSeedShouldNotAddASeedWhenLoadingFromStorage() {
        final TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();
        final Urn seed = Urn.forTrack(123L);

        operations.stationWithSeed(station, seed).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents().get(0).getTracks()).isEqualTo(stationFromDisk.getTracks());
    }

    @Test
    public void stationWithSeedShouldAddASeedWhenLoadingFromNetwork() {
        final TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();
        final ApiStation stationApi = StationFixtures.getApiStation(Urn.forTrackStation(123L), 10);
        final Urn seed = Urn.forTrack(123L);
        when(stationsStorage.station(station)).thenReturn(Observable.<StationRecord>empty());
        when(stationsApi.fetchStation(station)).thenReturn(Observable.just(stationApi));

        operations.stationWithSeed(station, seed).subscribe(subscriber);

        final StationRecord stationWithSeed = Station.stationWithSeedTrack(stationApi, seed);
        assertThat(subscriber.getOnNextEvents().get(0).getTracks()).isEqualTo(stationWithSeed.getTracks());
    }

    @Test
    public void stationWithSeedShouldNotAddASeedWhenTheTrackListIsEmpty() {
        final TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();
        final ApiStation stationApi = StationFixtures.getApiStation(Urn.forTrackStation(123L), 0);
        final Urn seed = Urn.forTrack(123L);
        when(stationsStorage.station(station)).thenReturn(Observable.<StationRecord>empty());
        when(stationsApi.fetchStation(station)).thenReturn(Observable.just(stationApi));

        operations.stationWithSeed(station, seed).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents().get(0).getTracks()).isEqualTo(stationApi.getTracks());
    }

    @Test
    public void getStationShouldFallbackToNetwork() {
        when(stationsStorage.station(station)).thenReturn(Observable.<StationRecord>empty());

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

        when(stationsApi.fetchStation(station)).thenReturn(Observable.just(stationApi));
        when(stationsStorage.loadPlayQueue(station, 2)).thenReturn(Observable.from(subTrackList));

        operations.fetchUpcomingTracks(station, 2).subscribe(subscriber);

        subscriber.assertValue(PlayQueue.fromStation(station, subTrackList));
    }

    @Test
    public void collectionShouldReturnFromStorageWhenSyncedBefore() {
        final StationRecord station = StationFixtures.getStation(Urn.forTrackStation(123L));
        final TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();
        final PublishSubject<SyncJobResult> syncResults = PublishSubject.create();

        when(syncStateStorage.hasSyncedBefore(Syncable.RECENT_STATIONS)).thenReturn(true);
        when(stationsStorage.getStationsCollection(StationsCollectionsTypes.RECENT)).thenReturn(Observable.just(station));
        when(syncInitiator.sync(Syncable.RECENT_STATIONS)).thenReturn(syncResults);

        operations.collection(StationsCollectionsTypes.RECENT).subscribe(subscriber);

        subscriber.assertValue(station);
        assertThat(syncResults.hasObservers()).isFalse();
    }

    @Test
    public void collectionShouldTriggerSyncerWhenNotSyncedBefore() {
        final PublishSubject<SyncJobResult> syncResults = PublishSubject.create();
        final StationRecord station = StationFixtures.getStation(Urn.forTrackStation(123L));
        final TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();
        when(stationsStorage.getStationsCollection(StationsCollectionsTypes.RECENT)).thenReturn(Observable.just(station));
        when(syncInitiator.sync(Syncable.RECENT_STATIONS)).thenReturn(syncResults);

        operations.collection(StationsCollectionsTypes.RECENT).subscribe(subscriber);

        subscriber.assertNoValues();
        syncResults.onNext(SyncJobResult.success("action", true));
        syncResults.onCompleted();
        subscriber.assertValue(station);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldPersistApiStation() {
        when(stationsStorage.station(station)).thenReturn(Observable.<StationRecord>empty());

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

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED,
                                                                   EntityStateChangedEvent.class);
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.STATIONS_COLLECTION_UPDATED);
        assertThat(event.getFirstUrn()).isEqualTo(station);
    }

    @Test
    public void shouldMarkMigrationAsCompletedIfSucceeded() {
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();

        when(stationsStorage.shouldRunRecentToLikedMigration()).thenReturn(true);
        when(stationsApi.requestRecentToLikedMigration()).thenReturn(Observable.just(true));

        operations.migrateRecentToLikedIfNeeded().subscribe(testSubscriber);

        verify(stationsStorage).markRecentToLikedMigrationComplete();
        testSubscriber.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    public void shouldNotMarkMigrationAsCompletedIfFailed() {
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();

        when(stationsStorage.shouldRunRecentToLikedMigration()).thenReturn(true);
        when(stationsApi.requestRecentToLikedMigration()).thenReturn(Observable.just(false));

        operations.migrateRecentToLikedIfNeeded().subscribe(testSubscriber);

        verify(stationsStorage, never()).markRecentToLikedMigrationComplete();
        testSubscriber.assertCompleted();
    }

    @Test
    public void shouldNotAttemptMigrationIfSucceedBefore() {
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();

        operations.migrateRecentToLikedIfNeeded().subscribe(testSubscriber);

        verify(stationsStorage, never()).markRecentToLikedMigrationComplete();
        verify(stationsApi, never()).requestRecentToLikedMigration();

        testSubscriber.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    public void shouldReturnOnboardingStreamItemWhenPreferenceIsEnabled() {
        when(stationsStorage.isOnboardingStreamItemDisabled()).thenReturn(false);
        final TestSubscriber<SoundStreamItem> subscriber = new TestSubscriber<>();
        operations.onboardingStreamItem().subscribe(subscriber);
        subscriber.assertValueCount(1);
        assertThat(subscriber.getOnNextEvents().get(0).kind()).isEqualTo(SoundStreamItem.Kind.STATIONS_ONBOARDING);
    }

    @Test
    public void shouldNotReturnOnboardingStreamItemWhenPreferenceIsDisabled() {
        when(stationsStorage.isOnboardingStreamItemDisabled()).thenReturn(true);
        final TestSubscriber<SoundStreamItem> subscriber = new TestSubscriber<>();
        operations.onboardingStreamItem().subscribe(subscriber);
        subscriber.assertNoValues();
    }
}
