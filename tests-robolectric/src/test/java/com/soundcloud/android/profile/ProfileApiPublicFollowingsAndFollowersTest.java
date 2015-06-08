package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.model.PagedRemoteCollection;
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
    private final TestObserver<PagedRemoteCollection> observer = new TestObserver<>();
    private final PublicApiUser publicApiUser1 = ModelFixtures.create(PublicApiUser.class);
    private final PublicApiUser publicApiUser2 = ModelFixtures.create(PublicApiUser.class);
    private final CollectionHolder<PublicApiUser> results = new CollectionHolder<>(
            Arrays.asList(
                    publicApiUser1,
                    publicApiUser2),
            NEXT_HREF);

    @Before
    public void setUp() throws Exception {
        api = new ProfileApiPublic(apiClientRx, storeTracksCommand, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void returnsUserFollowingsByUrnFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiUser>> results = Observable.just(this.results);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/users/123/followings")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);


        api.userFollowings(Urn.forUser(123L)).subscribe(observer);
        assertAllItemsEmitted();
    }

    @Test
    public void writesPlaylistsToDatabaseWhenRequestingFollowings() throws Exception {
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/users/123/followings")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(Observable.just(this.results));

        api.userFollowings(Urn.forUser(123L)).subscribe(observer);
        verify(storeUsersCommand).call(results);
    }

    @Test
    public void returnsUserFollowingsByNextPageLinkFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiUser>> results = Observable.just(this.results);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);

        api.userFollowings(NEXT_HREF).subscribe(observer);
        assertAllItemsEmitted();
    }

    @Test
    public void returnsUserFollowersByUrnFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiUser>> results = Observable.just(this.results);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/users/123/followers")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);


        api.userFollowers(Urn.forUser(123L)).subscribe(observer);
        assertAllItemsEmitted();
    }

    @Test
    public void writesPlaylistsToDatabaseWhenRequestingFollowers() throws Exception {
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", "/users/123/followers")
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(Observable.just(this.results));

        api.userFollowers(Urn.forUser(123L)).subscribe(observer);
        verify(storeUsersCommand).call(results);
    }

    @Test
    public void returnsUserFollowersByNextPageLinkFromApi() throws Exception {
        final Observable<CollectionHolder<PublicApiUser>> results = Observable.just(this.results);
        when(apiClientRx.mappedResponse(argThat(isPublicApiRequestTo("GET", NEXT_HREF)
                        .withQueryParam("linked_partitioning", "1")
                        .withQueryParam("limit", String.valueOf(ProfileApiPublic.PAGE_SIZE))),
                any(TypeToken.class))).thenReturn(results);

        api.userFollowers(NEXT_HREF).subscribe(observer);
        assertAllItemsEmitted();
    }

    private void assertAllItemsEmitted() {
        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).nextPageLink()).toEqual(Optional.of(NEXT_HREF));
        expect(observer.getOnNextEvents().get(0)).toContainExactly(
                publicApiUser1.toPropertySet(),
                publicApiUser2.toPropertySet()
        );
    }

}