package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.StoreProfileCommand.TO_RECORD_HOLDERS;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;

public class UserProfileOperationsTracksTest extends AndroidUnitTest {
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

    private final TestObserver<PagedRemoteCollection<PlayableItem>> observer = new TestObserver<>();
    private ApiTrack apiTrack;

    private ModelCollection<ApiPlayableSource> page;

    @Before
    public void setUp() {
        apiTrack = create(ApiTrack.class);
        page = new ModelCollection<>(Collections.singletonList(ApiPlayableSource.create(apiTrack, null)), NEXT_HREF);
        operations = new UserProfileOperations(
                profileApi,
                Schedulers.immediate(),
                loadPlaylistLikedStatuses,
                userRepository,
                writeMixedRecordsCommand,
                storeProfileCommand,
                storeUsersCommand,
                spotlightItemStatusLoader,
                ModelFixtures.entityItemCreator(),
                eventBus);
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

        verify(writeMixedRecordsCommand).call(TO_RECORD_HOLDERS(page));
    }

    @Test
    public void userTracksPagerStoresNextPage() {
        when(profileApi.userTracks(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.userTracksPagingFunction().call(new PagedRemoteCollection<>(Collections.emptyList(), NEXT_HREF)).subscribe(observer);

        verify(writeMixedRecordsCommand).call(TO_RECORD_HOLDERS(page));
    }

    private void assertAllItemsEmitted() {
        assertAllItemsEmitted(ModelFixtures.trackItem(apiTrack));
    }

    private void assertAllItemsEmitted(PlayableItem... playableItems) {
        final List<PagedRemoteCollection<PlayableItem>> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);
        assertThat(onNextEvents.get(0).nextPageLink().get()).isEqualTo(NEXT_HREF);
        assertThat(onNextEvents.get(0)).containsExactly(playableItems);
    }
}
