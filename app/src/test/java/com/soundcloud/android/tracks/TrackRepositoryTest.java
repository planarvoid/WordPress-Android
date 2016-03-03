package com.soundcloud.android.tracks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

public class TrackRepositoryTest extends AndroidUnitTest {

    public static final String TITLE = "title";
    public static final String CREATOR = "creator";
    public static final String DESCRIPTION = "Description...";

    private TrackRepository trackRepository;

    private Urn trackUrn = Urn.forTrack(123L);
    private Urn userUrn = Urn.forUser(123L);
    private PropertySet track;
    private PropertySet trackDescription;

    @Mock private TrackStorage trackStorage;
    @Mock private AccountOperations accountOperations;
    @Mock private SyncInitiator syncInitiator;

    private TestObserver<PropertySet> observer = new TestObserver<>();

    @Before
    public void setUp() {
        trackRepository = new TrackRepository(trackStorage, syncInitiator, Schedulers.immediate());
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

        assertThat(observer.getOnNextEvents()).containsExactly(track);
    }

    @Test
    public void trackUsesSyncerToBackfillMissingTrack() {
        final PropertySet syncedTrack = TestPropertySets.expectedTrackForPlayer();
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(PropertySet.create()), Observable.just(syncedTrack));

        trackRepository.track(trackUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(syncedTrack);
        verify(syncInitiator, times(2)).syncTrack(trackUrn);
    }

    @Test
    public void trackUsesSyncerToBackfillErrorOnLoad() {
        final PropertySet syncedTrack = TestPropertySets.expectedTrackForPlayer();
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.<PropertySet>error(new NullPointerException()), Observable.just(syncedTrack));

        trackRepository.track(trackUrn).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(syncedTrack);
        verify(syncInitiator).syncTrack(trackUrn);
    }

    @Test
    public void fullTrackWithUpdateReturnsTrackDetailsFromStorage() {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.<Boolean>empty());
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(track));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(trackDescription));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe(observer);

        final PropertySet first = observer.getOnNextEvents().get(0);
        assertThat(first.get(PlayableProperty.TITLE)).isEqualTo(TITLE);
        assertThat(first.get(PlayableProperty.CREATOR_NAME)).isEqualTo(CREATOR);
        assertThat(first.get(TrackProperty.DESCRIPTION)).isEqualTo(DESCRIPTION);
    }

    @Test
    public void fullTrackWithUpdateEmitsTrackFromStorageTwice() throws CreateModelException {
        when(syncInitiator.syncTrack(trackUrn)).thenReturn(Observable.just(true));
        when(trackStorage.loadTrack(trackUrn)).thenReturn(Observable.just(track));
        when(trackStorage.loadTrackDescription(trackUrn)).thenReturn(Observable.just(trackDescription));

        trackRepository.fullTrackWithUpdate(trackUrn).subscribe(observer);

        final PropertySet propertySet = track.merge(trackDescription);
        assertThat(observer.getOnNextEvents()).containsExactly(propertySet, propertySet);
        verify(trackStorage, times(2)).loadTrack(trackUrn);
    }

}
