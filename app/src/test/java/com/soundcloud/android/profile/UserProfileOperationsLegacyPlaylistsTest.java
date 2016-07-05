package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class UserProfileOperationsLegacyPlaylistsTest {

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
    @Captor private ArgumentCaptor<Iterable<ApiPlaylist>> playlistsCaptor;

    private final ApiPlaylist apiPlaylist1 = ModelFixtures.create(ApiPlaylist.class);
    private final ApiPlaylist apiPlaylist2 = ModelFixtures.create(ApiPlaylist.class);
    final TestObserver<PagedRemoteCollection> observer = new TestObserver<>();

    final ModelCollection<ApiPlaylist> page = new ModelCollection<>(
            Arrays.asList(
                    apiPlaylist1,
                    apiPlaylist2
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
                spotlightItemStatusLoader,
                eventBus);
    }

    @Test
    public void returnsUserPlaylistsResultFromApi() {
        when(profileApi.userLegacyPlaylists(USER_URN)).thenReturn(Observable.just(page));

        operations.legacyPagedPlaylists(USER_URN).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void userPlaylistsMergesInPlaylistLikeInfo() {
        when(profileApi.userLegacyPlaylists(USER_URN)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page)))).thenReturn(likedStatusForPlaylistLike(
                apiPlaylist2));

        operations.legacyPagedPlaylists(USER_URN).subscribe(observer);

        assertAllItemsEmittedWithLike();
    }

    @Test
    public void storesUserPlaylistsResultFromApi() {
        when(profileApi.userLegacyPlaylists(USER_URN)).thenReturn(Observable.just(page));

        operations.legacyPagedPlaylists(USER_URN).subscribe(observer);

        verify(writeMixedRecordsCommand).call(playlistsCaptor.capture());
        assertThat(playlistsCaptor.getValue()).containsExactly(apiPlaylist1, apiPlaylist2);
    }

    @Test
    public void userPlaylistsPagerReturnsNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userLegacyPlaylists(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.legacyPlaylistsPagingFunction().call(page1).subscribe(observer);

        assertAllItemsEmitted();
    }

    @Test
    public void userPlaylistsPagerMergesInPlaylistLikeInfo() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userLegacyPlaylists(NEXT_HREF)).thenReturn(Observable.just(page));
        when(loadPlaylistLikedStatuses.call(eq(new PagedRemoteCollection(page)))).thenReturn(likedStatusForPlaylistLike(
                apiPlaylist2));

        operations.legacyPlaylistsPagingFunction().call(page1).subscribe(observer);

        assertAllItemsEmittedWithLike();
    }

    @Test
    public void userPlaylistsPagerStoresNextPage() {
        final PagedRemoteCollection page1 = new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(),
                                                                      NEXT_HREF);
        when(profileApi.userLegacyPlaylists(NEXT_HREF)).thenReturn(Observable.just(page));

        operations.legacyPlaylistsPagingFunction().call(page1).subscribe(observer);

        verify(writeMixedRecordsCommand).call(playlistsCaptor.capture());
        assertThat(playlistsCaptor.getValue()).containsExactly(apiPlaylist1, apiPlaylist2);
    }

    private void assertAllItemsEmitted() {
        assertAllItemsEmitted(
                apiPlaylist1.toPropertySet(),
                apiPlaylist2.toPropertySet()
        );
    }

    private void assertAllItemsEmittedWithLike() {
        assertAllItemsEmitted(
                apiPlaylist1.toPropertySet(),
                apiPlaylist2.toPropertySet().put(PlayableProperty.IS_USER_LIKE, true)
        );
    }

    private void assertAllItemsEmitted(PropertySet... propertySets) {
        final List<PagedRemoteCollection> onNextEvents = observer.getOnNextEvents();
        assertThat(onNextEvents).hasSize(1);
        assertThat(onNextEvents.get(0).nextPageLink().get()).isEqualTo(NEXT_HREF);
        assertThat(onNextEvents.get(0)).containsExactly(propertySets);
    }

    @NotNull
    private Map<Urn, PropertySet> likedStatusForPlaylistLike(ApiPlaylist playlist2) {
        final PropertySet playlistIsLikedStatus = PropertySet.from(PlaylistProperty.IS_USER_LIKE.bind(true));
        return Collections.singletonMap(playlist2.getUrn(), playlistIsLikedStatus);
    }
}
