package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

public class ProfileApiMobileAlbumsTest extends AndroidUnitTest {

    private static final String NEXT_HREF = "next-href";

    @Mock private ApiClientRx apiClientRx;

    private ProfileApiMobile api;
    private final TestSubscriber<ModelCollection<ApiPlaylistPost>> subscriber = new TestSubscriber<>();
    private final ApiPlaylistPost playlistPost = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
    private final ModelCollection<ApiPlaylistPost> collection = new ModelCollection<>(
            newArrayList(playlistPost),
            NEXT_HREF);

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiMobile(apiClientRx);
    }

    @Test
    public void shouldReturnUserAlbumsByUrn() throws Exception {
        final Observable<ModelCollection<ApiPlaylistPost>> results = Observable.just(collection);
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/users/soundcloud%3Ausers%3A123/releases/posted")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                isA(TypeToken.class))).thenReturn(results);

        api.userAlbums(Urn.forUser(123L)).subscribe(subscriber);
        subscriber.assertValue(new ModelCollection<>(newArrayList(playlistPost), NEXT_HREF));
    }

    @Test
    public void shouldReturnUserAlbumsByNextPageLink() throws Exception {
        final Observable<ModelCollection<ApiPlaylistPost>> results = Observable.just(collection);
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                isA(TypeToken.class))).thenReturn(results);

        api.userAlbums(NEXT_HREF).subscribe(subscriber);
        subscriber.assertValue(new ModelCollection<>(newArrayList(playlistPost), NEXT_HREF));
    }
}
