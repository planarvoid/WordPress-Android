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
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.BulkStorage;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

@RunWith(SoundCloudTestRunner.class)
public class TrackOperationsTest {

    public static final String TITLE = "title";
    public static final String CREATOR = "creator";
    public static final String DESCRIPTION = "Description...";

    private TrackOperations trackOperations;

    private Urn trackUrn = Urn.forTrack(123L);
    private Urn userUrn = Urn.forUser(123L);
    private PropertySet track;
    private PropertySet trackDescription;
    private TestEventBus eventBus;

    @Mock private LoadTrackCommand loadTrack;
    @Mock private LoadTrackDescriptionCommand loadTrackDescription;
    @Mock private AccountOperations accountOperations;
    @Mock private BulkStorage bulkStorage;
    @Mock private SyncInitiator syncInitiator;

    private TestObserver<PropertySet> observer = new TestObserver<>();

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        trackOperations = new TrackOperations(loadTrack, loadTrackDescription, eventBus, syncInitiator);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        track = PropertySet.from(TrackProperty.URN.bind(trackUrn),
                PlayableProperty.TITLE.bind(TITLE),
                PlayableProperty.CREATOR_NAME.bind(CREATOR));

        trackDescription = PropertySet.from(TrackProperty.DESCRIPTION.bind(DESCRIPTION));
    }

    @Test
    public void trackReturnsTrackPropertySetByUrnWithLoggedInUserUrn() throws Exception {
        when(loadTrack.toObservable()).thenReturn(Observable.just(track));

        trackOperations.track(trackUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(track);
        expect(loadTrack.getInput()).toEqual(trackUrn);
    }

    @Test
    public void trackUsesSyncerToBackfillMissingTrack() {
        final PropertySet syncedTrack = TestPropertySets.expectedTrackForPlayer();
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(loadTrack.toObservable()).thenReturn(Observable.<PropertySet>empty(), Observable.just(syncedTrack));

        trackOperations.track(trackUrn).subscribe(observer);

        expect(loadTrack.getInput()).toEqual(trackUrn);
        expect(observer.getOnNextEvents()).toContainExactly(syncedTrack);
        verify(syncInitiator).syncTrack(trackUrn);
    }

    @Test
    public void fullTrackWithUpdateReturnsTrackDetailsFromStorage() {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.<Boolean>empty());
        when(loadTrack.toObservable()).thenReturn(Observable.just(track));
        when(loadTrackDescription.toObservable()).thenReturn(Observable.just(trackDescription));

        trackOperations.fullTrackWithUpdate(trackUrn).subscribe(observer);

        final PropertySet first = observer.getOnNextEvents().get(0);
        expect(loadTrack.getInput()).toEqual(trackUrn);
        expect(first.get(PlayableProperty.TITLE)).toEqual(TITLE);
        expect(first.get(PlayableProperty.CREATOR_NAME)).toEqual(CREATOR);
        expect(first.get(TrackProperty.DESCRIPTION)).toEqual(DESCRIPTION);
    }

    @Test
    public void fullTrackWithUpdateEmitsTrackFromStorageTwice() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(loadTrack.toObservable()).thenReturn(Observable.just(track));
        when(loadTrackDescription.toObservable()).thenReturn(Observable.just(trackDescription));

        trackOperations.fullTrackWithUpdate(trackUrn).subscribe(observer);

        final PropertySet propertySet = track.merge(trackDescription);
        expect(observer.getOnNextEvents()).toContainExactly(propertySet, propertySet);
        expect(loadTrack.getInput()).toEqual(trackUrn);
        verify(loadTrack, times(2)).toObservable();
    }

    @Test
    public void fullTrackWithUpdatePublishesPlayableChangedEvent() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(loadTrack.toObservable()).thenReturn(Observable.just(track));
        when(loadTrackDescription.toObservable()).thenReturn(Observable.just(trackDescription));

        trackOperations.fullTrackWithUpdate(trackUrn).subscribe();

        expect(loadTrack.getInput()).toEqual(trackUrn);
        expect(eventBus.lastEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet()).toEqual(
                track.merge(trackDescription));
    }
}
