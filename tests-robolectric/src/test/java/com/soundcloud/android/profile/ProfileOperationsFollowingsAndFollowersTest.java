package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.search.LoadPlaylistLikedStatuses;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ProfileOperationsFollowingsAndFollowersTest {

    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String NEXT_HREF = "next-href";

    private ProfileOperations operations;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Captor private ArgumentCaptor<Iterable<ApiUser>> userCaptor;

    private final ApiUser apiUser1 = ModelFixtures.create(ApiUser.class);
    private final ApiUser apiUser2 = ModelFixtures.create(ApiUser.class);
    final TestObserver<PagedRemoteCollection> observer = new TestObserver<>();

    final ModelCollection<ApiUser> page = new ModelCollection<>(
            Arrays.asList(
                    apiUser1,
                    apiUser2
            ),
            NEXT_HREF);

    @Before
    public void setUp() {
        operations = new ProfileOperations(profileApi, Schedulers.immediate(), loadPlaylistLikedStatuses, userRepository,
                storeTracksCommand, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void returnsUserFollowersResultFromApi() {
        when(profileApi.userFollowers(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedFollowers(USER_URN).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void storesUserFollowersResultFromApi() {
        when(profileApi.userFollowers(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedFollowers(USER_URN).subscribe(observer);

        verify(storeUsersCommand).call(userCaptor.capture());
        expect(userCaptor.getValue()).toContainExactly(apiUser1, apiUser2);
    }

    @Test
    public void userFollowersPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userFollowers(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.followersPagingFunction().call(page1).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void userFollowersPagerStoresNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userFollowers(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.followersPagingFunction().call(page1).subscribe(observer);

        verify(storeUsersCommand).call(userCaptor.capture());
        expect(userCaptor.getValue()).toContainExactly(apiUser1, apiUser2);
    }

    @Test
    public void returnsUserFollowingsResultFromApi() {
        when(profileApi.userFollowers(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedFollowers(USER_URN).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void storesUserFollowingsResultFromApi() {
        when(profileApi.userFollowers(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedFollowers(USER_URN).subscribe(observer);

        verify(storeUsersCommand).call(userCaptor.capture());
        expect(userCaptor.getValue()).toContainExactly(apiUser1, apiUser2);
    }

    @Test
    public void userFollowingsPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userFollowers(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.followersPagingFunction().call(page1).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void userFollowingsPagerStoresNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userFollowers(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.followersPagingFunction().call(page1).subscribe(observer);

        verify(storeUsersCommand).call(userCaptor.capture());
        expect(userCaptor.getValue()).toContainExactly(apiUser1, apiUser2);
    }

    private void assertAllItemsEmitted() {
        assertAllItemsEmitted(
                apiUser1.toPropertySet(),
                apiUser2.toPropertySet()
        );
    }

    private void assertAllItemsEmitted(PropertySet... propertySets) {
        final List<PagedRemoteCollection> onNextEvents = observer.getOnNextEvents();
        expect(onNextEvents).toNumber(1);
        expect(onNextEvents.get(0).nextPageLink()).toEqual(Optional.of(NEXT_HREF));
        expect(onNextEvents.get(0)).toContainExactly(propertySets);
    }
}