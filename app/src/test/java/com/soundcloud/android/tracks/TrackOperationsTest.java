package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
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

    @Mock
    private TrackStorage trackStorage;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private RxHttpClient rxHttpClient;

    @Before
    public void setUp() {
        trackOperations = new TrackOperations(trackStorage, accountOperations, rxHttpClient);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        propertySet = PropertySet.from(TrackProperty.URN.bind(trackUrn),
                PlayableProperty.TITLE.bind(TITLE),
                PlayableProperty.CREATOR_NAME.bind(CREATOR));
    }

    @Test
    public void trackReturnsTrackPropertySetByUrnWithLoggedInUserUrn() {
        when(trackStorage.track(trackUrn, userUrn)).thenReturn(Observable.just(propertySet));
        expect(trackOperations.track(trackUrn).toBlockingObservable().lastOrDefault(null)).toBe(propertySet);
    }

    @Test
    public void trackDetailsWithUpdateReturnsTrackDetailsFromStorage() {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        when(trackStorage.track(trackUrn, userUrn)).thenReturn(Observable.just(propertySet));

        final PropertySet first = trackOperations.trackDetailsWithUpdate(trackUrn).toBlocking().first();
        expect(first.get(PlayableProperty.TITLE)).toEqual(TITLE);
        expect(first.get(PlayableProperty.CREATOR_NAME)).toEqual(CREATOR);
    }

    @Test
    public void trackDetailsWithUpdateReturnsTrackDetailsFromApiLast() throws CreateModelException {
        final PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        track.description = DESCRIPTION;

        final Observable trackObservable = Observable.just(track);
        when(rxHttpClient.fetchModels(argThat(isPublicApiRequestTo("GET", "/tracks/123")))).thenReturn(trackObservable);
        when(trackStorage.track(any(TrackUrn.class), any(UserUrn.class))).thenReturn(Observable.<PropertySet>empty());

        final PropertySet last = trackOperations.trackDetailsWithUpdate(trackUrn).toBlocking().last();
        expect(last.get(PlayableProperty.TITLE)).toEqual(track.getTitle());
        expect(last.get(PlayableProperty.CREATOR_NAME)).toEqual(track.getUsername());
        expect(last.get(TrackProperty.DESCRIPTION)).toEqual(DESCRIPTION);
    }
}
