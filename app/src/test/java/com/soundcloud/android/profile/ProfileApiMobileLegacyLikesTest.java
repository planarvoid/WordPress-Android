package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.model.ApiPlaylist;
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

public class ProfileApiMobileLegacyLikesTest extends AndroidUnitTest {

    private static final String NEXT_HREF = "next-href";

    @Mock private ApiClientRx apiClientRx;

    private ProfileApiMobile api;
    private final TestSubscriber<ModelCollection<ApiEntityHolder>> subscriber = new TestSubscriber<>();
    private final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
    private final ModelCollection<ApiPlayableSource> apiLikeHolder = new ModelCollection<>(
            Arrays.asList(
                    new ApiPlayableSource(apiTrack, null),
                    new ApiPlayableSource(null, apiPlaylist),
                    new ApiPlayableSource(null, null)), // unkown type, futureproofing
            NEXT_HREF);

    @Before
    public void setUp() {
        api = new ProfileApiMobile(apiClientRx);
    }

    @Test
    public void returnsUserPostsByUrnFromApi() {
        final Observable<ModelCollection<ApiPlayableSource>> results = Observable.just(apiLikeHolder);
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/users/soundcloud%3Ausers%3A123/liked_tracks_and_playlists")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                isA(TypeToken.class))).thenReturn(results);

        api.legacyUserLikes(Urn.forUser(123L)).subscribe(subscriber);
        assertAllLikesEmitted();
    }

    @Test
    public void returnsUserPostsByNextPageLinkFromApi() {
        final Observable<ModelCollection<ApiPlayableSource>> results = Observable.just(apiLikeHolder);
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                isA(TypeToken.class))).thenReturn(results);

        api.legacyUserLikes(NEXT_HREF).subscribe(subscriber);
        assertAllLikesEmitted();
    }

    private void assertAllLikesEmitted() {
        final List<ApiEntityHolder> entities = Arrays.<ApiEntityHolder>asList(apiTrack, apiPlaylist);
        subscriber.assertValue(new ModelCollection<>(entities, NEXT_HREF));
    }

}
