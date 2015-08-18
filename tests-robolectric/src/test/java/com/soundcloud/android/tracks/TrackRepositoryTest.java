package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

@RunWith(SoundCloudTestRunner.class)
public class TrackRepositoryTest {

    public static final String TITLE = "title";
    public static final String CREATOR = "creator";
    public static final String DESCRIPTION = "Description...";

    private TrackRepository trackRepository;

    private Urn trackUrn = Urn.forTrack(123L);
    private Urn userUrn = Urn.forUser(123L);
    private PropertySet track;
    private PropertySet trackDescription;
    private TestEventBus eventBus;

    @Mock private TrackStorage trackStorage;
    @Mock private AccountOperations accountOperations;
    @Mock private SyncInitiator syncInitiator;

    private TestObserver<PropertySet> observer = new TestObserver<>();

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        trackRepository = new TrackRepository(trackStorage, eventBus, syncInitiator, Schedulers.immediate());
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        track = PropertySet.from(TrackProperty.URN.bind(trackUrn),
                PlayableProperty.TITLE.bind(TITLE),
                PlayableProperty.CREATOR_NAME.bind(CREATOR));

        trackDescription = PropertySet.from(TrackProperty.DESCRIPTION.bind(DESCRIPTION));
    }

    @Test
    public void trackReturnsTrackPropertySetByUrnWithLoggedInUserUrn() throws Exception {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(track));

        trackRepository.track(trackUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(track);
    }

    @Test
    public void trackUsesSyncerToBackfillMissingTrack() {
        final PropertySet syncedTrack = TestPropertySets.expectedTrackForPlayer();
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(PropertySet.create()), Observable.just(syncedTrack));

        trackRepository.track(trackUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(syncedTrack);
        verify(syncInitiator, times(2)).syncTrack(trackUrn);
    }

    @Test
    public void trackUsesSyncerToBackfillErrorOnLoad() {
        final PropertySet syncedTrack = TestPropertySets.expectedTrackForPlayer();
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.<PropertySet>error(new NullPointerException()), Observable.just(syncedTrack));

        trackRepository.track(trackUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(syncedTrack);
        verify(syncInitiator).syncTrack(trackUrn);
    }

    @Test
    public void fullTrackWithUpdateReturnsTrackDetailsFromStorage() {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.<Boolean>empty());
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(track));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(trackDescription));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe(observer);

        final PropertySet first = observer.getOnNextEvents().get(0);
        expect(first.get(PlayableProperty.TITLE)).toEqual(TITLE);
        expect(first.get(PlayableProperty.CREATOR_NAME)).toEqual(CREATOR);
        expect(first.get(TrackProperty.DESCRIPTION)).toEqual(DESCRIPTION);
    }

    @Test
    public void fullTrackWithUpdateEmitsTrackFromStorageTwice() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(track));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(trackDescription));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe(observer);

        final PropertySet propertySet = track.merge(trackDescription);
        expect(observer.getOnNextEvents()).toContainExactly(propertySet, propertySet);
        verify(trackStorage, times(2)).loadTrack(trackUrn);
    }

    @Test
    public void fullTrackWithUpdatePublishesPlayableChangedEvent() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(track));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(trackDescription));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe();

        expect(eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED).getNextChangeSet()).toEqual(
                track.merge(trackDescription));
    }
}
