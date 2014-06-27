package com.soundcloud.android.track;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserUrn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class TrackOperationsTest {

    private TrackOperations trackOperations;

    private TrackUrn trackUrn = Urn.forTrack(123L);
    private UserUrn userUrn = Urn.forUser(123L);
    private PropertySet propertySet = PropertySet.create(1);

    @Mock
    private TrackStorage trackStorage;
    @Mock
    private AccountOperations accountOperations;

    @Before
    public void setUp() {
        trackOperations = new TrackOperations(trackStorage, accountOperations);
    }

    @Test
    public void trackReturnsTrackPropertySetByUrnWithLoggedInUserUrn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
        when(trackStorage.track(trackUrn, userUrn)).thenReturn(Observable.just(propertySet));
        expect(trackOperations.track(trackUrn).toBlockingObservable().lastOrDefault(null)).toBe(propertySet);

    }

    @Test
    public void shouldSendAnErrorIfTrackOperationsRaiseAnException() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        Observer<PropertySet> observer = mock(Observer.class);

        trackOperations.track(trackUrn).subscribe(observer);

        verify(observer).onError(any(RuntimeException.class));
        verifyNoMoreInteractions(observer);
    }
}
