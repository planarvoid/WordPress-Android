package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.StoreProfileCommand.TO_RECORD_HOLDERS;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UserProfileOperationsLikesTest extends AndroidUnitTest {
    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String NEXT_HREF = "next-href";
    private static final Date CREATED_AT = new Date();

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
    private ApiTrack apiTrack;
    private ApiPlaylist apiPlaylist;

    ModelCollection<ApiPlayableSource> page;

    @Before
    public void setUp() {
        apiTrack = create(ApiTrack.class);
        apiPlaylist = create(ApiPlaylist.class);
        page = new ModelCollection<>(
                asList(ApiPlayableSource.create(apiTrack, null), ApiPlayableSource.create(null, apiPlaylist)),
                NEXT_HREF);
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
    public void returnsUserLikesResultFromApi() {
        when(profileApi.userLikes(USER_URN)).thenReturn(Observable.just(page));

        operations.userLikes(USER_URN).subscribe(subscriber);

        assertAllItemsEmitted();
    }

    @Test
    public void userLikesMergesInPlaylistLikeInfo() {
        when(profileApi.userLikes(USER_URN)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(Collections.singletonList(USER_URN)))
                .thenReturn(likedStatusForPlaylistLike(apiPlaylist));

        operations.userLikes(USER_URN).subscribe(subscriber);

        assertAllItemsEmittedWithLike();
    }

    @Test
    public void storesUserLikesResultFromApi() {
        when(profileApi.userLikes(USER_URN)).thenReturn(Observable.just(page));

        operations.userLikes(USER_URN).subscribe(subscriber);

        verify(writeMixedRecordsCommand).call(TO_RECORD_HOLDERS(page));
    }

    @Test
    public void userLikesPagerMergesInPlaylistLikeInfo() {
        final PagedRemoteCollection<PlayableItem> page1 = new PagedRemoteCollection<>(Collections.emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userLikes(NEXT_HREF)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(singletonList(apiPlaylist.getUrn())))
                .thenReturn(likedStatusForPlaylistLike(apiPlaylist));

        operations.likesPagingFunction().call(page1).subscribe(subscriber);

        assertAllItemsEmittedWithLike();
    }

    @Test
    public void userLikesPagerStoresNextPage() {
        final PagedRemoteCollection<PlayableItem> page1 = new PagedRemoteCollection<>(Collections.emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userLikes(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.likesPagingFunction().call(page1).subscribe(subscriber);

        verify(writeMixedRecordsCommand).call(TO_RECORD_HOLDERS(page));
    }

    private void assertAllItemsEmitted() {
        assertAllItemsEmitted(
                TrackItem.from(apiTrack),
                PlaylistItem.from(apiPlaylist)
        );
    }

    private void assertAllItemsEmittedWithLike() {
        final PlaylistItem playlistItem = PlaylistItem.from(apiPlaylist);
        assertAllItemsEmitted(
                TrackItem.from(apiTrack),
                playlistItem);
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
