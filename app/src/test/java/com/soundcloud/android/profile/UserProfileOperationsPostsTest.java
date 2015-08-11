package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiTrackRepost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.search.LoadPlaylistLikedStatuses;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UserProfileOperationsPostsTest extends AndroidUnitTest {

    private static final PropertySet USER = TestPropertySets.user();
    private static final Urn USER_URN = USER.get(UserProperty.URN);
    private static final String NEXT_HREF = "next-href";

    private UserProfileOperations operations;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;

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
        operations = new UserProfileOperations(profileApi, Schedulers.immediate(), loadPlaylistLikedStatuses, userRepository,
                writeMixedRecordsCommand);
        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(USER));
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

        verify(writeMixedRecordsCommand).call(page);
    }

    @Test
    public void userPostsPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.postsPagingFunction(USER_URN).call(page1).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void userPostsPagerMergesInPlaylistLikeInfo() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page)))).thenReturn(likedStatusForPlaylistLike(apiPlaylist2));

        operations.postsPagingFunction(USER_URN).call(page1).subscribe(observer);

        assertItemsEmittedWithLike();
    }

    @Test
    public void userPostsPagerStoresNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.postsPagingFunction(USER_URN).call(page1).subscribe(observer);

        verify(writeMixedRecordsCommand).call(page);
    }

    @Test
    public void postsForPlaybackReturnsPostsWithReposterInformation() {
        final TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();
        final TrackItem trackPostItem = TrackItem.from(apiTrackPost.toPropertySet());
        final TrackItem trackRepostItem = TrackItem.from(attachRepostInfo(apiTrackRepost.toPropertySet()));
        final PlaylistItem playlistPostItem = PlaylistItem.from(apiPlaylistPost.toPropertySet());
        final PlaylistItem playlistRepostItem = PlaylistItem.from(attachRepostInfo(apiPlaylistRepost.toPropertySet()));

        operations.postsForPlayback(
                Arrays.asList(
                        trackPostItem,
                        trackRepostItem,
                        playlistPostItem,
                        playlistRepostItem
                )
        ).subscribe(subscriber);

        subscriber.assertValues(
                Arrays.asList(
                        PropertySet.from(TrackProperty.URN.bind(trackPostItem.getEntityUrn())),
                        PropertySet.from(TrackProperty.URN.bind(trackRepostItem.getEntityUrn()), TrackProperty.REPOSTER_URN.bind(trackRepostItem.getReposterUrn()))
                )
        );
    }

    private void assertAllItemsEmitted() {
        assertItemsEmitted(
                apiTrackPost.toPropertySet(),
                attachRepostInfo(apiTrackRepost.toPropertySet()),
                apiPlaylistPost.toPropertySet(),
                attachRepostInfo(apiPlaylistRepost.toPropertySet())
        );
    }

    private void assertItemsEmittedWithLike() {
        assertItemsEmitted(
                apiTrackPost.toPropertySet(),
                attachRepostInfo(apiTrackRepost.toPropertySet()),
                apiPlaylistPost.toPropertySet(),
                attachRepostInfo(apiPlaylistRepost.toPropertySet().put(PlayableProperty.IS_LIKED, true))
        );
    }

    @NonNull
    private PropertySet attachRepostInfo(PropertySet propertySet) {
        return propertySet
                .put(PlayableProperty.REPOSTER_URN, USER_URN)
                .put(PlayableProperty.REPOSTER, USER.get(UserProperty.USERNAME));
    }

    private void assertItemsEmitted(PropertySet... propertySets) {
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