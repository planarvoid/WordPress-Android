package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiTrackRepost;
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
import java.util.Date;

public class ProfileApiMobilePostTest extends AndroidUnitTest {

    private static final String NEXT_HREF = "next-href";
    public static final Date REPOST_DATE = new Date();

    @Mock private ApiClientRx apiClientRx;

    private ProfileApiMobile api;
    private final TestSubscriber<ModelCollection<ApiEntityHolder>> subscriber = new TestSubscriber<>();
    private final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
    private final ModelCollection<ApiPostSource> apiMobileHolder = new ModelCollection<>(
            Arrays.asList(
                    new ApiPostSource(new ApiTrackPost(apiTrack), null, null, null),
                    new ApiPostSource(null, new ApiTrackRepost(apiTrack, REPOST_DATE), null, null),
                    new ApiPostSource(null, null, new ApiPlaylistPost(apiPlaylist), null),
                    new ApiPostSource(null, null, null, new ApiPlaylistRepost(apiPlaylist, REPOST_DATE)),
                    new ApiPostSource(null, null, null, null)), // unkown type, futureproofing
            NEXT_HREF);

    @Before
    public void setUp() {
        api = new ProfileApiMobile(apiClientRx);
    }

    @Test
    public void returnsUserPostsByUrnFromApi() {
        final Observable<ModelCollection<ApiPostSource>> results = Observable.just(apiMobileHolder);
        when(apiClientRx.<ModelCollection<ApiPostSource>>mappedResponse(argThat(isApiRequestTo("GET",
                                                                               "/users/soundcloud%3Ausers%3A123/posted_and_reposted_tracks_and_playlists")
                                                                        .withQueryParam("limit",
                                                                                        String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                                        isA(TypeToken.class))).thenReturn(results);

        api.userPosts(Urn.forUser(123L)).subscribe(subscriber);
        assertAllPostsEmitted();
    }

    @Test
    public void returnsUserPostsByNextPageLinkFromApi() {
        final Observable<ModelCollection<ApiPostSource>> results = Observable.just(apiMobileHolder);
        when(apiClientRx.<ModelCollection<ApiPostSource>>mappedResponse(argThat(isApiRequestTo("GET", NEXT_HREF)
                                                                        .withQueryParam("limit",
                                                                                        String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                                        isA(TypeToken.class))).thenReturn(results);

        api.userPosts(NEXT_HREF).subscribe(subscriber);
        assertAllPostsEmitted();
    }

    private void assertAllPostsEmitted() {
        subscriber.assertReceivedOnNext(Arrays.asList(new ModelCollection<>(Arrays.asList(new ApiTrackPost(apiTrack),
                                                                                          new ApiTrackRepost(apiTrack,
                                                                                                             REPOST_DATE),
                                                                                          new ApiPlaylistPost(
                                                                                                  apiPlaylist),
                                                                                          new ApiPlaylistRepost(
                                                                                                  apiPlaylist,
                                                                                                  REPOST_DATE)),
                                                                            NEXT_HREF)));
    }

}
