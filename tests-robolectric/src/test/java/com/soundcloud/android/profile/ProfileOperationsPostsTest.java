package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiTrackRepost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PlayableProperty;
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
import java.util.Date;
import java.util.List;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class ProfileOperationsPostsTest {

    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String NEXT_HREF = "next-href";

    private ProfileOperations operations;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;

    final TestObserver<PagedRemoteCollection> observer = new TestObserver<>();

    private final ApiPlaylist apiPlaylist1 = ModelFixtures.create(ApiPlaylist.class);
    private final ApiPlaylist apiPlaylist2 = ModelFixtures.create(ApiPlaylist.class);
    private final ApiTrackPost apiTrackPost = new ApiTrackPost(ModelFixtures.create(ApiTrack.class));
    private final ApiTrackRepost apiTrackRepost = new ApiTrackRepost(ModelFixtures.create(ApiTrack.class), new Date());
    private final ApiPlaylistPost apiPlaylistPost = new ApiPlaylistPost(apiPlaylist1);
    private final ApiPlaylistRepost apiPlaylistRepost = new ApiPlaylistRepost(apiPlaylist2, new Date());

    final ModelCollection<PropertySetSource> page = new ModelCollection<>(
            Arrays.asList(
                    apiTrackPost,
                    apiTrackRepost,
                    apiPlaylistPost,
                    apiPlaylistRepost
            ),
            NEXT_HREF);


    @Before
    public void setUp() {
        operations = new ProfileOperations(profileApi, Schedulers.immediate(), loadPlaylistLikedStatuses, userRepository,
                storeTracksCommand, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void returnsUserPostsResultFromApi() {
        when(profileApi.userPosts(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedPostItems(USER_URN).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void userPostsMergesInPlaylistLikeInfo() {
        when(profileApi.userPosts(USER_URN)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page)))).thenReturn(likedStatusForPlaylistLike(apiPlaylist2));

        operations.pagedPostItems(USER_URN).subscribe(observer);

        assertItemsEmittedWithLike();
    }

    @Test
    public void storesUserPostsResultFromApi() {
        when(profileApi.userPosts(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedPostItems(USER_URN).subscribe(observer);

        verify(storeTracksCommand).call(Arrays.asList(apiTrackPost.getTrackRecord(), apiTrackRepost.getTrackRecord()));
        verify(storePlaylistsCommand).call(Arrays.asList(apiPlaylistPost.getPlaylistRecord(), apiPlaylistRepost.getPlaylistRecord()));
    }

    @Test
    public void userPostsPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.postsPagingFunction().call(page1).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void userPostsPagerMergesInPlaylistLikeInfo() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page)))).thenReturn(likedStatusForPlaylistLike(apiPlaylist2));

        operations.postsPagingFunction().call(page1).subscribe(observer);

        assertItemsEmittedWithLike();
    }

    @Test
    public void userPostsPagerStoresNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.postsPagingFunction().call(page1).subscribe(observer);

        verify(storeTracksCommand).call(Arrays.asList(apiTrackPost.getTrackRecord(), apiTrackRepost.getTrackRecord()));
        verify(storePlaylistsCommand).call(Arrays.asList(apiPlaylistPost.getPlaylistRecord(), apiPlaylistRepost.getPlaylistRecord()));
    }

    private void assertAllItemsEmitted() {
        assertItemsEmitted(
                apiTrackPost.toPropertySet(),
                apiTrackRepost.toPropertySet(),
                apiPlaylistPost.toPropertySet(),
                apiPlaylistRepost.toPropertySet()
        );
    }

    private void assertItemsEmittedWithLike() {
        assertItemsEmitted(
                apiTrackPost.toPropertySet(),
                apiTrackRepost.toPropertySet(),
                apiPlaylistPost.toPropertySet(),
                apiPlaylistRepost.toPropertySet().put(PlayableProperty.IS_LIKED, true)
        );
    }

    private void assertItemsEmitted(PropertySet... propertySets) {
        final List<PagedRemoteCollection> onNextEvents = observer.getOnNextEvents();
        expect(onNextEvents).toNumber(1);
        expect(onNextEvents.get(0).nextPageLink()).toEqual(Optional.of(NEXT_HREF));
        expect(onNextEvents.get(0)).toContainExactly(propertySets);
    }

    @NotNull
    private Map<Urn, PropertySet> likedStatusForPlaylistLike(ApiPlaylist playlist2) {
        final PropertySet playlistIsLikedStatus = PropertySet.from(PlaylistProperty.IS_LIKED.bind(true));
        return Collections.singletonMap(playlist2.getUrn(), playlistIsLikedStatus);
    }
}