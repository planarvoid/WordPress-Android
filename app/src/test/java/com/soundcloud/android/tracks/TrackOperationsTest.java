package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
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
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

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

    @Mock private TrackStorage trackStorage;
    @Mock private AccountOperations accountOperations;
    @Mock private BulkStorage bulkStorage;
    @Mock private SyncInitiator syncInitiator;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        trackOperations = new TrackOperations(trackStorage, accountOperations, eventBus, syncInitiator);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        track = PropertySet.from(TrackProperty.URN.bind(trackUrn),
                PlayableProperty.TITLE.bind(TITLE),
                PlayableProperty.CREATOR_NAME.bind(CREATOR));

        trackDescription = PropertySet.from(TrackProperty.DESCRIPTION.bind(DESCRIPTION));
    }

    @Test
    public void trackReturnsTrackPropertySetByUrnWithLoggedInUserUrn() {
        when(trackStorage.track(trackUrn, userUrn)).thenReturn(Observable.just(track));
        expect(trackOperations.track(trackUrn).toBlocking().last()).toBe(track);
    }

    @Test
    public void trackUsesSyncerToBackfillMissingTrack() {
        final Observable<PropertySet> emptyObservable = Observable.empty();
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.track(trackUrn, userUrn)).thenReturn(emptyObservable);

        expect(trackOperations.track(trackUrn).toBlocking().toIterable()).toBeEmpty();

        verify(trackStorage, times(2)).track(trackUrn, userUrn);
    }

    @Test
    public void fullTrackWithUpdateReturnsTrackDetailsFromStorage() {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.<Boolean>empty());
        when(trackStorage.track(trackUrn, userUrn)).thenReturn(Observable.just(track));
        when(trackStorage.trackDetails(trackUrn)).thenReturn(Observable.just(trackDescription));

        final PropertySet first = trackOperations.fullTrackWithUpdate(trackUrn).toBlocking().first();
        expect(first.get(PlayableProperty.TITLE)).toEqual(TITLE);
        expect(first.get(PlayableProperty.CREATOR_NAME)).toEqual(CREATOR);
        expect(first.get(TrackProperty.DESCRIPTION)).toEqual(DESCRIPTION);
    }

    @Test
    public void fullTrackWithUpdateEmitsTrackFromStorageTwice() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.track(any(Urn.class), any(Urn.class))).thenReturn(Observable.just(track));
        when(trackStorage.trackDetails(any(Urn.class))).thenReturn(Observable.just(trackDescription));

        expect(trackOperations.fullTrackWithUpdate(trackUrn).toBlocking().last()).toEqual(
                track.merge(trackDescription));

        verify(trackStorage, times(2)).track(trackUrn, userUrn);
    }

    @Test
    public void fullTrackWithUpdatePublishesPlayableChangedEvent() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.track(any(Urn.class), any(Urn.class))).thenReturn(Observable.just(track));
        when(trackStorage.trackDetails(any(Urn.class))).thenReturn(Observable.just(trackDescription));

        expect(trackOperations.fullTrackWithUpdate(trackUrn).toBlocking().last()).toEqual(
                track.merge(trackDescription));

        expect(eventBus.lastEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet()).toEqual(
                track.merge(trackDescription));
    }
}
