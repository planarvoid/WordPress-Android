package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.StoreProfileCommand.TO_RECORD_HOLDERS;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UserProfileOperationsRepostsTest extends AndroidUnitTest {
    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String NEXT_HREF = "next-href";

    private UserProfileOperations operations;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;
    @Mock private StoreProfileCommand storeProfileCommand;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private SpotlightItemStatusLoader spotlightItemStatusLoader;
    @Mock private EventBus eventBus;

    final TestSubscriber<PagedRemoteCollection<PlayableItem>> subscriber = new TestSubscriber<>();
    private TrackItem trackItem;
    private ApiPlaylist apiPlaylist;
    private PlaylistItem playlistItem;
    private ModelCollection<ApiPlayableSource> page;
    private List<Urn> pageUrns;

    @Before
    public void setUp() {
        ApiTrack apiTrack = create(ApiTrack.class);
        ApiPlayableSource apiTrackPlayableSource = ApiPlayableSource.create(apiTrack, null);
        trackItem = TrackItem.from(apiTrack, true);
        apiPlaylist = create(ApiPlaylist.class);
        ApiPlayableSource apiPlaylistPlayableSource = ApiPlayableSource.create(null, apiPlaylist);
        playlistItem = PlaylistItem.from(apiPlaylist, true);
        page = new ModelCollection<>(
                asList(apiTrackPlayableSource, apiPlaylistPlayableSource),
                NEXT_HREF);
        pageUrns = Lists.newArrayList(apiTrack.getUrn(), apiPlaylist.getUrn());

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
    }

    @Test
    public void returnsUserRepostsResultFromApi() {
        when(profileApi.userReposts(USER_URN)).thenReturn(Observable.just(page));

        operations.userReposts(USER_URN).subscribe(subscriber);

        assertAllItemsEmitted();
    }

    @Test
    public void userLikesMergesInPlaylistLikeInfo() {
        when(profileApi.userReposts(USER_URN)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(pageUrns)))
                .thenReturn(likedStatusForPlaylistLike(apiPlaylist));

        operations.userReposts(USER_URN).subscribe(subscriber);

        assertAllItemsEmittedWithLikeAndRepost();
    }

    @Test
    public void storesUserRepostsResultFromApi() {
        when(profileApi.userReposts(USER_URN)).thenReturn(Observable.just(page));

        operations.userReposts(USER_URN).subscribe(subscriber);

        verify(writeMixedRecordsCommand).call(TO_RECORD_HOLDERS(page));
    }

    @Test
    public void userRepostsPagerMergesInPlaylistLikeInfo() {
        final PagedRemoteCollection<PlayableItem> page1 = new PagedRemoteCollection<>(Collections.<PlayableItem>emptyList(),
                                                                                      NEXT_HREF);
        when(profileApi.userReposts(NEXT_HREF)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(pageUrns)))
                .thenReturn(likedStatusForPlaylistLike(apiPlaylist));

        operations.repostsPagingFunction().call(page1).subscribe(subscriber);

        assertAllItemsEmittedWithLikeAndRepost();
    }

    @Test
    public void userRepostsPagerStoresNextPage() {
        final PagedRemoteCollection<ApiPlayableSource> page1 = new PagedRemoteCollection<>(Collections.<ApiPlayableSource>emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userReposts(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.repostsPagingFunction().call(page1.transform(PlayableItem::from)).subscribe(subscriber);

        verify(writeMixedRecordsCommand).call(TO_RECORD_HOLDERS(page));
    }

    private void assertAllItemsEmitted() {
        assertAllItemsEmitted(
                trackItem,
                playlistItem
        );
    }

    private void assertAllItemsEmittedWithLikeAndRepost() {
        playlistItem.setLikedByCurrentUser(true);
        assertAllItemsEmitted(
                trackItem,
                playlistItem
        );
    }

    private void assertAllItemsEmitted(PlayableItem... playableItems) {
        final List<PagedRemoteCollection<PlayableItem>> onNextEvents = subscriber.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);
        assertThat(onNextEvents.get(0).nextPageLink().get()).isEqualTo(NEXT_HREF);
        assertThat(onNextEvents.get(0)).containsExactly(playableItems);
    }

    private Map<Urn, Boolean> likedStatusForPlaylistLike(ApiPlaylist playlist) {
        return Collections.singletonMap(playlist.getUrn(), true);
    }
}
