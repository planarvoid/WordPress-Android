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
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UserProfileOperationsTracksTest extends AndroidUnitTest {
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

    final TestObserver<PagedRemoteCollection> observer = new TestObserver<>();
    final ApiTrack apiTrack = create(ApiTrack.class);

    final ModelCollection<ApiEntityHolder> page = new ModelCollection<>(
            Collections.<ApiEntityHolder>singletonList(apiTrack),
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
    public void returnsUserTracksResultFromApi() {
        when(profileApi.userTracks(USER_URN)).thenReturn(Observable.just(page));

        operations.userTracks(USER_URN).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void storesUserTracksResultFromApi() {
        when(profileApi.userTracks(USER_URN)).thenReturn(Observable.just(page));

        operations.userTracks(USER_URN).subscribe(observer);

        verify(writeMixedRecordsCommand).call(page);
    }

    @Test
    public void userTracksPagerStoresNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userTracks(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.userTracksPagingFunction().call(page1).subscribe(observer);

        verify(writeMixedRecordsCommand).call(page);
    }

    private void assertAllItemsEmitted() {
        assertAllItemsEmitted(
                apiTrack.toPropertySet()
        );
    }

    private void assertAllItemsEmitted(PropertySet... propertySets) {
        final List<PagedRemoteCollection> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);
        assertThat(onNextEvents.get(0).nextPageLink().get()).isEqualTo(NEXT_HREF);
        assertThat(onNextEvents.get(0)).containsExactly(propertySets);
    }
}
