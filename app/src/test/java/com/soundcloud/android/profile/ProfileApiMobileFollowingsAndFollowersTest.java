package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileApi.PAGE_SIZE;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
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

public class ProfileApiMobileFollowingsAndFollowersTest extends AndroidUnitTest {

    private static final String NEXT_HREF = "next-href";
    private final TestSubscriber<ModelCollection<ApiUser>> testSubscriber = new TestSubscriber<>();
    private final ApiUser apiUser1 = ModelFixtures.create(ApiUser.class);
    private final ApiUser apiUser2 = ModelFixtures.create(ApiUser.class);
    private final ModelCollection<ApiUser> apiCollection = new ModelCollection<>(
            Arrays.asList(
                    apiUser1,
                    apiUser2),
            NEXT_HREF);
    private final Observable<ModelCollection<ApiUser>> results = Observable.just(apiCollection);
    @Mock private ApiClientRx apiClientRx;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;
    private ProfileApiMobile api;

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiMobile(apiClientRx);
    }

    @Test
    public void returnsUserFollowingsByUrnFromApi() throws Exception {
        when(apiClientRx.<ModelCollection<ApiUser>>mappedResponse(argThat(isApiRequestTo("GET", "/followings/soundcloud%3Ausers%3A123/users")
                                                                                  .withQueryParam("linked_partitioning", "1")
                                                                                  .withQueryParam("limit", String.valueOf(PAGE_SIZE))),
                                                                  any(TypeToken.class))).thenReturn(results);

        api.userFollowings(Urn.forUser(123L)).subscribe(testSubscriber);
        assertAllItemsEmitted();
    }

    @Test
    public void returnsUserFollowingsByNextPageLinkFromApi() throws Exception {
        when(apiClientRx.<ModelCollection<ApiUser>>mappedResponse(argThat(isApiRequestTo("GET", NEXT_HREF)
                                                                                  .withQueryParam("linked_partitioning", "1")
                                                                                  .withQueryParam("limit", String.valueOf(PAGE_SIZE))),
                                                                  any(TypeToken.class))).thenReturn(results);

        api.userFollowings(NEXT_HREF).subscribe(testSubscriber);
        assertAllItemsEmitted();
    }

    @Test
    public void returnsUserFollowersByUrnFromApi() throws Exception {
        when(apiClientRx.<ModelCollection<ApiUser>>mappedResponse(argThat(isApiRequestTo("GET", "/followers/soundcloud%3Ausers%3A123/users")
                                                                                  .withQueryParam("linked_partitioning", "1")
                                                                                  .withQueryParam("limit", String.valueOf(PAGE_SIZE))),
                                                                  any(TypeToken.class))).thenReturn(results);

        api.userFollowers(Urn.forUser(123L)).subscribe(testSubscriber);
        assertAllItemsEmitted();
    }

    @Test
    public void returnsUserFollowersByNextPageLinkFromApi() throws Exception {
        when(apiClientRx.<ModelCollection<ApiUser>>mappedResponse(argThat(isApiRequestTo("GET", NEXT_HREF)
                                                                                  .withQueryParam("linked_partitioning", "1")
                                                                                  .withQueryParam("limit", String.valueOf(PAGE_SIZE))),
                                                                  any(TypeToken.class))).thenReturn(results);

        api.userFollowers(NEXT_HREF).subscribe(testSubscriber);
        assertAllItemsEmitted();
    }

    private void assertAllItemsEmitted() {
        assertThat(testSubscriber.getOnNextEvents()).hasSize(1);
        assertThat(testSubscriber.getOnNextEvents().get(0).getNextLink().get().getHref()).isEqualTo(NEXT_HREF);
        assertThat(testSubscriber.getOnNextEvents().get(0).getCollection()).contains(apiUser1, apiUser2);
    }

}
