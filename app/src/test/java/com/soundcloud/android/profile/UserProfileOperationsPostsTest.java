package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.StoreProfileCommand.TO_RECORD_HOLDERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiTrackRepost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
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

    private static final User USER = ModelFixtures.user();
    private static final Urn USER_URN = USER.urn();
    private static final String NEXT_HREF = "next-href";

    private UserProfileOperations operations;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;
    @Mock private StoreProfileCommand storeProfileCommand;
    @Mock private SpotlightItemStatusLoader spotlightItemStatusLoader;
    @Mock private EventBus eventBus;

    final TestObserver<PagedRemoteCollection<PlayableItem>> observer = new TestObserver<>();

    private final ApiTrack apiTrack1 = ModelFixtures.create(ApiTrack.class);
    private final ApiTrack apiTrack2 = ModelFixtures.create(ApiTrack.class);
    private final ApiPlaylist apiPlaylist1 = ModelFixtures.create(ApiPlaylist.class);
    private final ApiPlaylist apiPlaylist2 = ModelFixtures.create(ApiPlaylist.class);

    private final TrackItem trackPostItem = TrackItem.from(apiTrack1, false);
    private final TrackItem trackRepostItem = TrackItem.from(apiTrack2, true);
    private final PlaylistItem playlistPostItem = PlaylistItem.from(apiPlaylist1, false);
    private final PlaylistItem playlistRepostItem = PlaylistItem.from(apiPlaylist2, true);

    private List<Urn> postUrns = Lists.newArrayList(apiTrack1.getUrn(),
                                                         apiTrack2.getUrn(),
                                                         apiPlaylist1.getUrn(),
                                                         apiPlaylist2.getUrn());

    private final ApiTrackPost apiTrackPost = new ApiTrackPost(apiTrack1);
    private final ApiTrackRepost apiTrackRepost = new ApiTrackRepost(apiTrack2, new Date());
    private final ApiPlaylistPost apiPlaylistPost = new ApiPlaylistPost(apiPlaylist1);
    private final ApiPlaylistRepost apiPlaylistRepost = new ApiPlaylistRepost(apiPlaylist2, new Date());
    private final ModelCollection<ApiPostSource> page = new ModelCollection<>(
            Arrays.asList(
                    ApiPostSource.create(apiTrackPost, null, null, null),
                    ApiPostSource.create(null, apiTrackRepost, null, null),
                    ApiPostSource.create(null, null, apiPlaylistPost, null),
                    ApiPostSource.create(null, null, null, apiPlaylistRepost)
            ),
            NEXT_HREF);



    @Before
    public void setUp() {
        operations = new UserProfileOperations(
                profileApi,
                Schedulers.immediate(),
                loadPlaylistLikedStatuses,
                userRepository,
                writeMixedRecordsCommand,
                storeProfileCommand,
                storeUsersCommand,
                spotlightItemStatusLoader,
                eventBus);

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
        when(loadPlaylistLikedStatuses.call(eq(postUrns))).thenReturn(likedStatusForPlaylistLike(
                apiPlaylist2));

        operations.pagedPostItems(USER_URN).subscribe(observer);

        assertItemsEmittedWithLike();
    }

    @Test
    public void storesUserPostsResultFromApi() {
        when(profileApi.userPosts(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedPostItems(USER_URN).subscribe(observer);

        verify(writeMixedRecordsCommand).call(TO_RECORD_HOLDERS(page));
    }

    @Test
    public void userPostsPagerReturnsNextPage() {
        final PagedRemoteCollection<PlayableItem> page1 = new PagedRemoteCollection<>(Collections.emptyList(),
                                                                                       NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.postsPagingFunction(USER_URN).call(page1).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void userPostsPagerMergesInPlaylistLikeInfo() {
        final PagedRemoteCollection<PlayableItem> page1 = new PagedRemoteCollection<>(Collections.emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(postUrns))).thenReturn(likedStatusForPlaylistLike(
                apiPlaylist2));

        operations.postsPagingFunction(USER_URN).call(page1).subscribe(observer);

        assertItemsEmittedWithLike();
    }

    @Test
    public void userPostsPagerStoresNextPage() {
        final PagedRemoteCollection<PlayableItem> page1 = new PagedRemoteCollection<>(Collections.emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.postsPagingFunction(USER_URN).call(page1).subscribe(observer);

        verify(writeMixedRecordsCommand).call(TO_RECORD_HOLDERS(page));
    }

    @Test
    public void postsForPlaybackReturnsPostsWithReposterInformation() {
        trackRepostItem.setReposterUrn(USER_URN);
        playlistRepostItem.setReposterUrn(USER_URN);
        final TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();

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
                        PropertySet.from(TrackProperty.URN.bind(trackPostItem.getUrn())),
                        PropertySet.from(TrackProperty.URN.bind(trackRepostItem.getUrn()),
                                         PostProperty.REPOSTER_URN.bind(trackRepostItem.getReposterUrn().get())),
                        PropertySet.from(PlaylistProperty.URN.bind(playlistPostItem.getUrn())),
                        PropertySet.from(PlaylistProperty.URN.bind(playlistRepostItem.getUrn()),
                                         PostProperty.REPOSTER_URN.bind(trackRepostItem.getReposterUrn().get()))
                )
        );
    }

    private void assertAllItemsEmitted() {
        assertItemsEmitted(
                trackPostItem,
                attachRepostInfo(trackRepostItem),
                playlistPostItem,
                attachRepostInfo(playlistRepostItem)
        );
    }

    private void assertItemsEmittedWithLike() {
        playlistRepostItem.setLikedByCurrentUser(true);
        assertItemsEmitted(
                trackPostItem,
                attachRepostInfo(trackRepostItem),
                playlistPostItem,
                attachRepostInfo(playlistRepostItem)
        );
    }

    @NonNull
    private PlayableItem attachRepostInfo(PlayableItem playableItem) {
        playableItem.setReposterUrn(USER_URN);
        playableItem.setReposter(USER.username());
        return playableItem;
    }

    private void assertItemsEmitted(PlayableItem... playableItems) {
        final List<PagedRemoteCollection<PlayableItem>> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);
        assertThat(onNextEvents.get(0).nextPageLink().get()).isEqualTo(NEXT_HREF);
        assertThat(onNextEvents.get(0)).containsExactly(playableItems);
    }

    @NotNull
    private Map<Urn, Boolean> likedStatusForPlaylistLike(ApiPlaylist playlist2) {
        return Collections.singletonMap(playlist2.getUrn(), true);
    }
}
