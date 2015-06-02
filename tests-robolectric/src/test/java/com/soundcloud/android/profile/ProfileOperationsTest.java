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
    public void setUp() throws Exception {
        operations = new ProfileOperations(profileApi, Schedulers.immediate(), loadPlaylistLikedStatuses, userRepository);
    }

    @Test
    public void returnsUserPostsResultFromApi() throws Exception {
        final PagedRemoteCollection page = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPosts(USER_URN)).thenReturn(Observable.just(page));

        operations.pagedPostItems(USER_URN).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page);
    }

    @Test
    public void userPostsPagerReturnsNextPage() throws Exception {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        final PagedRemoteCollection page2 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), NEXT_HREF);
        when(profileApi.userPosts(NEXT_HREF)).thenReturn(Observable.just(page2));

        operations.pagingFunction().call(page1).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(page2);
    }

    @Test
    public void userPostsMergesInPlaylistLikeInfo() throws Exception {
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

    @NotNull
    private Map<Urn, PropertySet> likedStatusForPlaylistLike(ApiPlaylist playlist2) {
        final PropertySet playlistIsLikedStatus = PropertySet.from(PlaylistProperty.IS_LIKED.bind(true));
        return Collections.singletonMap(playlist2.getUrn(), playlistIsLikedStatus);
    }
}