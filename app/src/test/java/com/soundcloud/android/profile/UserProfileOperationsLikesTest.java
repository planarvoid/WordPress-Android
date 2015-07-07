package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistLike;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackLike;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.search.LoadPlaylistLikedStatuses;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class UserProfileOperationsLikesTest {

    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String NEXT_HREF = "next-href";
    private static final Date CREATED_AT = new Date();

    private UserProfileOperations operations;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;

    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
    final TestObserver<PagedRemoteCollection> observer = new TestObserver<>();
    final ApiTrackLike apiTrackLike = new ApiTrackLike(ModelFixtures.create(ApiTrack.class), CREATED_AT);
    final ApiPlaylistLike apiPlaylistLike = new ApiPlaylistLike(apiPlaylist, CREATED_AT);

    final ModelCollection<PropertySetSource> page = new ModelCollection<>(
            Arrays.asList(
                    apiTrackLike,
                    apiPlaylistLike
            ),
            NEXT_HREF);

    @Before
    public void setUp() {
        operations = new UserProfileOperations(profileApi, Schedulers.immediate(), loadPlaylistLikedStatuses, userRepository,
                storeTracksCommand, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void returnsUserLikesResultFromApi() {
        when(profileApi.userLikes(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedLikes(USER_URN).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void userLikesMergesInPlaylistLikeInfo() {
        when(profileApi.userLikes(USER_URN)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page)))).thenReturn(likedStatusForPlaylistLike(apiPlaylist));

        operations.pagedLikes(USER_URN).subscribe(observer);

        assertAllItemsEmittedWithLike();
    }

    @Test
    public void storesUserLikesResultFromApi() {
        when(profileApi.userLikes(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedLikes(USER_URN).subscribe(observer);

        verify(storeTracksCommand).call(Arrays.asList(apiTrackLike.getTrackRecord()));
        verify(storePlaylistsCommand).call(Arrays.asList(apiPlaylistLike.getPlaylistRecord()));
    }

    @Test
    public void userLikesPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userLikes(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.likesPagingFunction().call(page1).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void userLikesPagerMergesInPlaylistLikeInfo() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userLikes(NEXT_HREF)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page)))).thenReturn(likedStatusForPlaylistLike(apiPlaylist));

        operations.likesPagingFunction().call(page1).subscribe(observer);

        assertAllItemsEmittedWithLike();
    }

    @Test
    public void userLikesPagerStoresNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userLikes(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.likesPagingFunction().call(page1).subscribe(observer);

        verify(storeTracksCommand).call(Arrays.asList(apiTrackLike.getTrackRecord()));
        verify(storePlaylistsCommand).call(Arrays.asList(apiPlaylistLike.getPlaylistRecord()));
    }

    private void assertAllItemsEmitted() {
        assertAllItemsEmitted(
                apiTrackLike.toPropertySet(),
                apiPlaylistLike.toPropertySet()
        );
    }

    private void assertAllItemsEmittedWithLike() {
        assertAllItemsEmitted(
                apiTrackLike.toPropertySet(),
                apiPlaylistLike.toPropertySet().put(PlayableProperty.IS_LIKED, true)
        );
    }

    private void assertAllItemsEmitted(PropertySet... propertySets) {
        final List<PagedRemoteCollection> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);
        assertThat(onNextEvents.get(0).nextPageLink().get()).isEqualTo(NEXT_HREF);
        assertThat(onNextEvents.get(0)).containsExactly(propertySets);
    }

    @NotNull
    private Map<Urn, PropertySet> likedStatusForPlaylistLike(ApiPlaylist playlist2) {
        final PropertySet playlistIsLikedStatus = PropertySet.from(PlaylistProperty.IS_LIKED.bind(true));
        return Collections.singletonMap(playlist2.getUrn(), playlistIsLikedStatus);
    }
}