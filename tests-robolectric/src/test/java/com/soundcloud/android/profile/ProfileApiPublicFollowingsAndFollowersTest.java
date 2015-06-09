package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ProfileApiPublicFollowingsAndFollowersTest {

    private static final String NEXT_HREF = "next-href";

    @Mock private ApiClientRx apiClientRx;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;

    private ProfileApiPublic api;
    private final TestObserver<ModelCollection<ApiUser>> observer = new TestObserver<>();
    private final PublicApiUser publicApiUser1 = ModelFixtures.create(PublicApiUser.class);
    private final PublicApiUser publicApiUser2 = ModelFixtures.create(PublicApiUser.class);
    private final CollectionHolder<PublicApiUser> publicApiCollection = new CollectionHolder<>(
            Arrays.asList(
                    publicApiUser1,
                    publicApiUser2),
            NEXT_HREF);

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiPublic(apiClientRx);
    }

    @Test
    public void returnsUserFollowingsByUrnFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiUser>> results = Observable.just(publicApiCollection);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/users/123/followings")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);

        api.userFollowings(Urn.forUser(123L)).subscribe(observer);
        assertAllItemsEmitted();
    }

    @Test
    public void returnsUserFollowingsByNextPageLinkFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiUser>> results = Observable.just(publicApiCollection);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);

        api.userFollowings(NEXT_HREF).subscribe(observer);
        assertAllItemsEmitted();
    }

    @Test
    public void returnsUserFollowersByUrnFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiUser>> results = Observable.just(publicApiCollection);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/users/123/followers")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);

        api.userFollowers(Urn.forUser(123L)).subscribe(observer);
        assertAllItemsEmitted();
    }

    @Test
    public void returnsUserFollowersByNextPageLinkFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiUser>> results = Observable.just(publicApiCollection);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);

        api.userFollowers(NEXT_HREF).subscribe(observer);
        assertAllItemsEmitted();
    }

    private void assertAllItemsEmitted() {
        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).getNextLink().get().getHref()).toEqual(NEXT_HREF);
        expect(observer.getOnNextEvents().get(0).getCollection()).toContain(
                publicApiUser1.toApiMobileUser(),
                publicApiUser2.toApiMobileUser()
        );
    }

}