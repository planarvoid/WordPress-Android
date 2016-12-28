package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
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

public class UserProfileOperationsPlaylistsTest extends AndroidUnitTest {
    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String NEXT_HREF = "next-href";

    private UserProfileOperations operations;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;
    @Mock private StoreProfileCommand storeProfileCommand;
    @Mock private SpotlightItemStatusLoader spotlightItemStatusLoader;
    @Mock private EventBus eventBus;

    final TestSubscriber<PagedRemoteCollection<PlaylistItem>> subscriber = new TestSubscriber<>();
    final ApiPlaylist apiPlaylist = create(ApiPlaylist.class);
    final ApiPlaylistPost apiPlaylistPost = new ApiPlaylistPost(apiPlaylist);
    final PlaylistItem playlistItem = PlaylistItem.from(apiPlaylist, false);
    final PlaylistItem playlistItemWithoutRepost = PlaylistItem.from(apiPlaylist);

    final ModelCollection<ApiPlaylistPost> page = new ModelCollection<>(singletonList(apiPlaylistPost), NEXT_HREF);

    @Before
    public void setUp() {
        operations = new UserProfileOperations(
                profileApi,
                Schedulers.immediate(),
                loadPlaylistLikedStatuses,
                userRepository,
                writeMixedRecordsCommand,
                storeProfileCommand,
                spotlightItemStatusLoader,
                eventBus);
    }

    @Test
    public void returnsUserPlaylistsResultFromApi() throws Exception {
        when(profileApi.userPlaylists(USER_URN)).thenReturn(Observable.just(page));

        operations.userPlaylists(USER_URN).subscribe(subscriber);

        assertResponse(playlistItem);
    }

    @Test
    public void mergesLikesInfoForUserPlaylists() throws Exception {
        when(profileApi.userPlaylists(USER_URN)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(Collections.singletonList(playlistItem.getUrn()))))
                .thenReturn(addLikeStatusToPlaylist());

        operations.userPlaylists(USER_URN).subscribe(subscriber);

        playlistItem.setLikedByCurrentUser(true);
        assertResponse(playlistItem);
    }

    @Test
    public void mergesLikesInfoForUserPlaylistsWithPagination() throws Exception {
        final PagedRemoteCollection<PlaylistItem> page1 = new PagedRemoteCollection<>(Collections.emptyList(),
                                                                                      NEXT_HREF);
        when(profileApi.userPlaylists(NEXT_HREF)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(Collections.singletonList(playlistItemWithoutRepost.getUrn()))))
                .thenReturn(addLikeStatusToPlaylist());

        operations.pagingFunction(nextPage -> operations.userPlaylists(nextPage)).call(page1).subscribe(subscriber);
        playlistItemWithoutRepost.setLikedByCurrentUser(true);
        assertResponse(playlistItemWithoutRepost);
    }

    @Test
    public void storesUserPlaylistsResultsFromApi() throws Exception {
        when(profileApi.userPlaylists(USER_URN)).thenReturn(Observable.just(page));

        operations.userPlaylists(USER_URN).subscribe(subscriber);

        verify(writeMixedRecordsCommand).call(page);
    }

    private Map<Urn, Boolean> addLikeStatusToPlaylist() {
        return Collections.singletonMap(apiPlaylist.getUrn(), true);
    }

    private void assertResponse(PlaylistItem playlistItem) {
        List<PagedRemoteCollection<PlaylistItem>> onNextEvents = subscriber.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);
        assertThat(onNextEvents.get(0).nextPageLink().get()).isEqualTo(NEXT_HREF);
        assertThat(onNextEvents.get(0)).containsExactly(playlistItem);
    }
}
