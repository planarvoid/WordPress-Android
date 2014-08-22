package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.BulkStorage;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.users.UserUrn;
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

    private TrackUrn trackUrn = Urn.forTrack(123L);
    private UserUrn userUrn = Urn.forUser(123L);
    private PropertySet propertySet;
    private PropertySet descriptionPropertySet;
    private TestEventBus eventBus;

    @Mock private TrackStorage trackStorage;
    @Mock private AccountOperations accountOperations;
    @Mock private RxHttpClient rxHttpClient;
    @Mock private BulkStorage bulkStorage;
    @Mock private SyncInitiator syncInitiator;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        trackOperations = new TrackOperations(trackStorage, accountOperations, eventBus, syncInitiator);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        propertySet = PropertySet.from(TrackProperty.URN.bind(trackUrn),
                PlayableProperty.TITLE.bind(TITLE),
                PlayableProperty.CREATOR_NAME.bind(CREATOR));

        descriptionPropertySet = PropertySet.from(TrackProperty.DESCRIPTION.bind(DESCRIPTION));
    }

    @Test
    public void trackReturnsTrackPropertySetByUrnWithLoggedInUserUrn() {
        when(trackStorage.track(trackUrn, userUrn)).thenReturn(Observable.just(propertySet));
        expect(trackOperations.track(trackUrn).toBlocking().last()).toBe(propertySet);
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
        when(trackStorage.track(trackUrn, userUrn)).thenReturn(Observable.just(propertySet));
        when(trackStorage.trackDetails(trackUrn)).thenReturn(Observable.just(descriptionPropertySet));

        final PropertySet first = trackOperations.fullTrackWithUpdate(trackUrn).toBlocking().first();
        expect(first.get(PlayableProperty.TITLE)).toEqual(TITLE);
        expect(first.get(PlayableProperty.CREATOR_NAME)).toEqual(CREATOR);
        expect(first.get(TrackProperty.DESCRIPTION)).toEqual(DESCRIPTION);
    }

    @Test
    public void fullTrackWithUpdateEmitsTrackFromStorageTwice() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.track(any(TrackUrn.class), any(UserUrn.class))).thenReturn(Observable.just(propertySet));
        when(trackStorage.trackDetails(any(TrackUrn.class))).thenReturn(Observable.just(descriptionPropertySet));

        expect(trackOperations.fullTrackWithUpdate(trackUrn).toBlocking().last()).toBe(propertySet);

        verify(trackStorage, times(2)).track(trackUrn, userUrn);
    }

    @Test
    public void fullTrackWithUpdatePublishesPlayableChangedEvent() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.track(any(TrackUrn.class), any(UserUrn.class))).thenReturn(Observable.just(propertySet));
        when(trackStorage.trackDetails(any(TrackUrn.class))).thenReturn(Observable.just(descriptionPropertySet));

        expect(trackOperations.fullTrackWithUpdate(trackUrn).toBlocking().last()).toBe(propertySet);

        expect(eventBus.lastEventOn(EventQueue.PLAYABLE_CHANGED).getChangeSet()).toEqual(propertySet);
    }
}
