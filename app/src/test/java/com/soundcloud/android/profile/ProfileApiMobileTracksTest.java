package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.List;

public class ProfileApiMobileTracksTest extends AndroidUnitTest {

    private static final String NEXT_HREF = "next-href";

    @Mock private ApiClientRx apiClientRx;

    private ProfileApiMobile api;
    private final TestSubscriber<ModelCollection<ApiEntityHolder>> subscriber = new TestSubscriber<>();
    private final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
    private ModelCollection<ApiPlayableSource> apiTracksHolder = new ModelCollection<>(
            newArrayList(
                    new ApiPlayableSource(apiTrack, null),
                    new ApiPlayableSource(null, null)
            ),
            NEXT_HREF);

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiMobile(apiClientRx);
    }

    @Test
    public void shouldReturnUserTracksByUrn() throws Exception {
        final Observable<ModelCollection<ApiPlayableSource>> results = Observable.just(apiTracksHolder);
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/users/soundcloud%3Ausers%3A123/tracks/posted")
                                                        .withQueryParam("limit",
                                                                        String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                                        isA(TypeToken.class))).thenReturn(results);

        api.userTracks(Urn.forUser(123L)).subscribe(subscriber);
        assertAllTracksEmitted();
    }

    @Test
    public void shouldReturnUserTracksByNextPageLink() {
        final Observable<ModelCollection<ApiPlayableSource>> results = Observable.just(apiTracksHolder);
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", NEXT_HREF)
                                                        .withQueryParam("limit",
                                                                        String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                                        isA(TypeToken.class))).thenReturn(results);

        api.userTracks(NEXT_HREF).subscribe(subscriber);
        assertAllTracksEmitted();
    }

    private void assertAllTracksEmitted() {
        final List<ApiEntityHolder> entities = Arrays.<ApiEntityHolder>asList(apiTrack);
        subscriber.assertValue(new ModelCollection<>(entities, NEXT_HREF));
    }
}
