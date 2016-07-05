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
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
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

public class UserProfileOperationsAlbumsTest extends AndroidUnitTest {
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

    final TestSubscriber<PagedRemoteCollection> subscriber = new TestSubscriber<>();
    final ApiPlaylist apiPlaylist = create(ApiPlaylist.class);
    final ApiPlaylistPost apiPlaylistPost = new ApiPlaylistPost(apiPlaylist);

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
    public void returnsUserAlbumsResultFromApi() throws Exception {
        when(profileApi.userAlbums(USER_URN)).thenReturn(Observable.just(page));

        operations.userAlbums(USER_URN).subscribe(subscriber);

        assertResponse(apiPlaylistPost.toPropertySet());
    }

    @Test
    public void mergesLikesInfoForUserAlbums() throws Exception {
        when(profileApi.userAlbums(USER_URN)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page))))
                .thenReturn(addLikeStatusToPlaylist());

        operations.userAlbums(USER_URN).subscribe(subscriber);

        assertResponse(apiPlaylistPost.toPropertySet().put(PlayableProperty.IS_USER_LIKE, true));
    }

    @Test
    public void mergesLikesInfoForUserAlbumsWithPagination() throws Exception {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userAlbums(NEXT_HREF)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page))))
                .thenReturn(addLikeStatusToPlaylist());

        operations.userAlbumsPagingFunction().call(page1).subscribe(subscriber);

        assertResponse(apiPlaylistPost.toPropertySet().put(PlayableProperty.IS_USER_LIKE, true));
    }

    @Test
    public void storesUserAlbumsResultsFromApi() throws Exception {
        when(profileApi.userAlbums(USER_URN)).thenReturn(Observable.just(page));

        operations.userAlbums(USER_URN).subscribe(subscriber);

        verify(writeMixedRecordsCommand).call(page);
    }

    private Map<Urn, PropertySet> addLikeStatusToPlaylist() {
        final PropertySet playlistIsLikedStatus = PropertySet.from(PlaylistProperty.IS_USER_LIKE.bind(true));
        return Collections.singletonMap(apiPlaylist.getUrn(), playlistIsLikedStatus);
    }

    private void assertResponse(PropertySet propertySet) {
        List<PagedRemoteCollection> onNextEvents = subscriber.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);
        assertThat(onNextEvents.get(0).nextPageLink().get()).isEqualTo(NEXT_HREF);
        assertThat(onNextEvents.get(0)).containsExactly(propertySet);
    }
}
