package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.LoadPlaylistLikedStatuses;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class UserProfileOperationsFollowingsAndFollowersTest {

    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String NEXT_HREF = "next-href";

    private UserProfileOperations operations;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;
    @Captor private ArgumentCaptor<Iterable<PropertySetSource>> userCaptor;

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
        operations = new UserProfileOperations(profileApi, Schedulers.immediate(), loadPlaylistLikedStatuses, userRepository,
                writeMixedRecordsCommand);
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

        verify(writeMixedRecordsCommand).call(userCaptor.capture());
        assertThat(userCaptor.getValue()).containsExactly(apiUser1, apiUser2);
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

        verify(writeMixedRecordsCommand).call(userCaptor.capture());
        assertThat(userCaptor.getValue()).containsExactly(apiUser1, apiUser2);
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

        verify(writeMixedRecordsCommand).call(userCaptor.capture());
        assertThat(userCaptor.getValue()).containsExactly(apiUser1, apiUser2);
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

        verify(writeMixedRecordsCommand).call(userCaptor.capture());
        assertThat(userCaptor.getValue()).containsExactly(apiUser1, apiUser2);
    }

    private void assertAllItemsEmitted() {
        assertAllItemsEmitted(
                apiUser1.toPropertySet(),
                apiUser2.toPropertySet()
        );
    }

    private void assertAllItemsEmitted(PropertySet... propertySets) {
        final List<PagedRemoteCollection> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);
        assertThat(onNextEvents.get(0).nextPageLink().get()).isEqualTo(NEXT_HREF);
        assertThat(onNextEvents.get(0)).containsExactly(propertySets);
    }
}