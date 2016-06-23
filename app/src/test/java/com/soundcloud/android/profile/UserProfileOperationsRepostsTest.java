package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackRepost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
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

public class UserProfileOperationsRepostsTest extends AndroidUnitTest {
    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String NEXT_HREF = "next-href";
    private static final Date CREATED_AT = new Date();

    private UserProfileOperations operations;

    @Mock private ProfileApi profileApi;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private UserRepository userRepository;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;
    @Mock private StoreProfileCommand storeProfileCommand;
    @Mock private SpotlightItemStatusLoader spotlightItemStatusLoader;

    final TestSubscriber<PagedRemoteCollection> subscriber = new TestSubscriber<>();
    final ApiTrackRepost apiTrackRepost = new ApiTrackRepost(create(ApiTrack.class), CREATED_AT);
    final ApiPlaylist apiPlaylist = create(ApiPlaylist.class);
    final ApiPlaylistRepost apiPlaylistRepost = new ApiPlaylistRepost(apiPlaylist, CREATED_AT);

    final ModelCollection<ApiEntityHolder> page = new ModelCollection<>(
            asList(apiTrackRepost, apiPlaylistRepost),
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
                spotlightItemStatusLoader);
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
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page))))
                .thenReturn(likedStatusForPlaylistLike(apiPlaylist));

        operations.userReposts(USER_URN).subscribe(subscriber);

        assertAllItemsEmittedWithLike();
    }

    @Test
    public void storesUserRepostsResultFromApi() {
        when(profileApi.userReposts(USER_URN)).thenReturn(Observable.just(page));

        operations.userReposts(USER_URN).subscribe(subscriber);

        verify(writeMixedRecordsCommand).call(page);
    }

    @Test
    public void userRepostsPagerMergesInPlaylistLikeInfo() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userReposts(NEXT_HREF)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page))))
                .thenReturn(likedStatusForPlaylistLike(apiPlaylist));

        operations.repostsPagingFunction().call(page1).subscribe(subscriber);

        assertAllItemsEmittedWithLike();
    }

    @Test
    public void userRepostsPagerStoresNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userReposts(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.repostsPagingFunction().call(page1).subscribe(subscriber);

        verify(writeMixedRecordsCommand).call(page);
    }

    private void assertAllItemsEmitted() {
        assertAllItemsEmitted(
                apiTrackRepost.toPropertySet(),
                apiPlaylistRepost.toPropertySet()
        );
    }

    private void assertAllItemsEmittedWithLike() {
        assertAllItemsEmitted(
                apiTrackRepost.toPropertySet(),
                apiPlaylistRepost.toPropertySet().put(PlayableProperty.IS_USER_LIKE, true)
        );
    }

    private void assertAllItemsEmitted(PropertySet... propertySets) {
        final List<PagedRemoteCollection> onNextEvents = subscriber.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);
        assertThat(onNextEvents.get(0).nextPageLink().get()).isEqualTo(NEXT_HREF);
        assertThat(onNextEvents.get(0)).containsExactly(propertySets);
    }

    private Map<Urn, PropertySet> likedStatusForPlaylistLike(ApiPlaylist playlist) {
        final PropertySet playlistIsLikedStatus = PropertySet.from(PlaylistProperty.IS_USER_LIKE.bind(true));
        return Collections.singletonMap(playlist.getUrn(), playlistIsLikedStatus);
    }
}
