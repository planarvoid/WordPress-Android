package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileApi.PAGE_SIZE;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;

public class ProfileApiMobileRepostsTest extends AndroidUnitTest {

    private static final String NEXT_HREF = "next-href";

    @Mock private ApiClientRx apiClientRx;

    private ProfileApiMobile api;
    private final TestSubscriber<ModelCollection<ApiPlayableSource>> subscriber = new TestSubscriber<>();
    private final ApiTrack apiTrack = TrackFixtures.apiTrack();
    private final ApiPlaylist apiPlaylist = PlaylistFixtures.apiPlaylist();
    private ModelCollection<ApiPlayableSource> apiRepostsHolder = new ModelCollection<>(
            newArrayList(
                    ApiPlayableSource.create(apiTrack, null),
                    ApiPlayableSource.create(null, apiPlaylist),
                    ApiPlayableSource.create(null, null)
            ),
            NEXT_HREF);

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiMobile(apiClientRx);
    }

    @Test
    public void shouldReturnUserRepostsByUrn() throws Exception {
        final Observable<ModelCollection<ApiPlayableSource>> results = Observable.just(apiRepostsHolder);
        when(apiClientRx.<ModelCollection<ApiPlayableSource>>mappedResponse(argThat(isApiRequestTo("GET", "/users/soundcloud%3Ausers%3A123/reposts")
                                                                        .withQueryParam("limit",
                                                                                        String.valueOf(PAGE_SIZE))),
                                        isA(TypeToken.class))).thenReturn(results);

        api.userReposts(Urn.forUser(123L)).subscribe(subscriber);
        assertAllRepostsEmitted();
    }

    @Test
    public void shouldReturnUserRepostsByNextPageLink() {
        final Observable<ModelCollection<ApiPlayableSource>> results = Observable.just(apiRepostsHolder);
        when(apiClientRx.<ModelCollection<ApiPlayableSource>>mappedResponse(argThat(isApiRequestTo("GET", NEXT_HREF)
                                                                        .withQueryParam("limit",
                                                                                        String.valueOf(PAGE_SIZE))),
                                        isA(TypeToken.class))).thenReturn(results);

        api.userReposts(NEXT_HREF).subscribe(subscriber);
        assertAllRepostsEmitted();
    }

    private void assertAllRepostsEmitted() {
        final List<ApiPlayableSource> collection = subscriber.getOnNextEvents().get(0).getCollection();
        assertThat(collection.get(0)).isEqualTo(ApiPlayableSource.create(apiTrack, null));
        assertThat(collection.get(1)).isEqualTo(ApiPlayableSource.create(null, apiPlaylist));
    }
}
