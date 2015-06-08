package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.search.LoadPlaylistLikedStatuses;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class ProfileOperationsTest {

    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String NEXT_HREF = "next-href";

    private ProfileOperations operations;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;

    final TestObserver<PagedRemoteCollection> observer = new TestObserver<>();

    @Before
    public void setUp() {
        operations = new ProfileOperations(profileApi, Schedulers.immediate(), loadPlaylistLikedStatuses, userRepository);
    }

    @Test
    public void returnsUserPostsResultFromApi() {
        final PagedRemoteCollection page = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPosts(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedPostItems(USER_URN).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page);
    }

    @Test
    public void userPostsPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        final PagedRemoteCollection page2 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page2));

        operations.postsPagingFunction().call(page1).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page2);
    }

    @Test
    public void userPostsMergesInPlaylistLikeInfo() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylist playlist2 = ModelFixtures.create(ApiPlaylist.class);
        final PagedRemoteCollection page = new PagedRemoteCollection(Arrays.asList(
                playlist,
                track,
                playlist2
        ), NEXT_HREF);

        when(profileApi.userPosts(USER_URN)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(page)).thenReturn(likedStatusForPlaylistLike(playlist2));

        operations.pagedPostItems(USER_URN).subscribe(observer);

        final List<PagedRemoteCollection> onNextEvents = observer.getOnNextEvents();
        expect(onNextEvents.get(0)).toContainExactly(
                playlist.toPropertySet(),
                track.toPropertySet(),
                playlist2.toPropertySet().put(PlaylistProperty.IS_LIKED,true));

    }

    @Test
    public void returnsUserLikesResultFromApi() {
        final PagedRemoteCollection page = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userLikes(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedLikes(USER_URN).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page);
    }

    @Test
    public void userLikesPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        final PagedRemoteCollection page2 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userLikes(NEXT_HREF)).thenReturn(Observable.just(page2));

        operations.likesPagingFunction().call(page1).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page2);
    }

    @Test
    public void userLikesMergesInPlaylistLikeInfo() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        final ApiPlaylist playlist2 = ModelFixtures.create(ApiPlaylist.class);
        final PagedRemoteCollection page = new PagedRemoteCollection(Arrays.asList(
                playlist,
                track,
                playlist2
        ), NEXT_HREF);

        when(profileApi.userLikes(USER_URN)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(page)).thenReturn(likedStatusForPlaylistLike(playlist2));

        operations.pagedLikes(USER_URN).subscribe(observer);

        final List<PagedRemoteCollection> onNextEvents = observer.getOnNextEvents();
        expect(onNextEvents.get(0)).toContainExactly(
                playlist.toPropertySet(),
                track.toPropertySet(),
                playlist2.toPropertySet().put(PlaylistProperty.IS_LIKED,true));

    }

    @Test
    public void returnsUserPlaylistsResultFromApi() {
        final PagedRemoteCollection page = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPlaylists(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedPlaylists(USER_URN).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page);
    }

    @Test
    public void userPlaylistsPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        final PagedRemoteCollection page2 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPlaylists(NEXT_HREF)).thenReturn(Observable.just(page2));

        operations.playlistsPagingFunction().call(page1).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page2);
    }

    @Test
    public void userPlaylistsMergesInPlaylistLikeInfo() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        final ApiPlaylist playlist2 = ModelFixtures.create(ApiPlaylist.class);
        final PagedRemoteCollection page = new PagedRemoteCollection(Arrays.asList(
                playlist,
                playlist2
        ), NEXT_HREF);

        when(profileApi.userPlaylists(USER_URN)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(page)).thenReturn(likedStatusForPlaylistLike(playlist2));

        operations.pagedPlaylists(USER_URN).subscribe(observer);

        final List<PagedRemoteCollection> onNextEvents = observer.getOnNextEvents();
        expect(onNextEvents.get(0)).toContainExactly(
                playlist.toPropertySet(),
                playlist2.toPropertySet().put(PlaylistProperty.IS_LIKED,true));
    }

    @Test
    public void returnsUserFollowingsResultFromApi() {
        final PagedRemoteCollection page = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userFollowings(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedFollowings(USER_URN).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page);
    }

    @Test
    public void userFollowingsPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        final PagedRemoteCollection page2 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userFollowings(NEXT_HREF)).thenReturn(Observable.just(page2));

        operations.followingsPagingFunction().call(page1).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page2);
    }

    @Test
    public void returnsUserFollowersResultFromApi() {
        final PagedRemoteCollection page = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userFollowers(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedFollowers(USER_URN).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page);
    }

    @Test
    public void userFollowersPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        final PagedRemoteCollection page2 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userFollowers(NEXT_HREF)).thenReturn(Observable.just(page2));

        operations.followersPagingFunction().call(page1).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page2);
    }

    @NotNull
    private Map<Urn, PropertySet> likedStatusForPlaylistLike(ApiPlaylist playlist2) {
        final PropertySet playlistIsLikedStatus = PropertySet.from(PlaylistProperty.IS_LIKED.bind(true));
        return Collections.singletonMap(playlist2.getUrn(), playlistIsLikedStatus);
    }
}