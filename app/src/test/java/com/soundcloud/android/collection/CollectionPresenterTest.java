package com.soundcloud.android.collection;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.util.Collections;
import java.util.List;

public class CollectionPresenterTest extends AndroidUnitTest {

    private static final int PLAYLIST_COUNT = 2;
    private static final int ZERO_PLAYLIST_COUNT = 0;
    private static final List<StationRecord> RECENT_STATIONS = singletonList(mock(StationRecord.class));
    private static final LikesItem LIKES = LikesItem.fromTrackPreviews(singletonList(
            LikedTrackPreview.create(Urn.forTrack(123L), "http://image-url")));

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private CollectionPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private CollectionOperations collectionOperations;
    @Mock private CollectionOptionsStorage collectionOptionsStorage;
    @Mock private CollectionPlaylistOptionsPresenter optionsPresenter;
    @Mock private CollectionAdapter adapter;
    @Mock private Fragment fragment;

    private TestEventBus eventBus = new TestEventBus();
    private PlaylistsOptions options;

    @Before
    public void setUp() throws Exception {
        when(collectionOperations.collections(any(PlaylistsOptions.class))).thenReturn(Observable.<MyCollection>empty());
        when(collectionOperations.onCollectionChanged()).thenReturn(Observable.empty());
        options = PlaylistsOptions.builder().build();
        when(collectionOptionsStorage.getLastOrDefault()).thenReturn(options);
        presenter = new CollectionPresenter(swipeRefreshAttacher,
                                            collectionOperations,
                                            collectionOptionsStorage,
                                            adapter,
                                            optionsPresenter,
                                            resources(),
                                            eventBus);
    }

    @Test
    public void updatesStoredOptionsWhenOptionsUpdated() {
        final PlaylistsOptions options = PlaylistsOptions.builder().build();
        presenter.onOptionsUpdated(options);
        verify(collectionOptionsStorage).store(options);
    }

    @Test
    public void usesFilterFromStorageForInitialLoad() {
        final MyCollection myCollection = getMyCollection(
                ModelFixtures.create(PlaylistItem.class, 2),
                LIKES,
                RECENT_STATIONS, false);

        when(collectionOptionsStorage.getLastOrDefault()).thenReturn(options);
        when(collectionOperations.collections(any(PlaylistsOptions.class))).thenReturn(Observable.just(myCollection));
        presenter.onCreate(fragment, null);

        verify(collectionOperations).collections(options);
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForLikes() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                PreviewCollectionItem.forLikesAndStations(myCollection.getLikes(), myCollection.getStations()),
                PlaylistHeaderCollectionItem.create(PLAYLIST_COUNT),
                PlaylistCollectionItem.create(playlistItems.get(0)),
                PlaylistCollectionItem.create(playlistItems.get(1)),
                PlaylistRemoveFilterCollectionItem.create()
        );
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForPosts() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showPosts(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                PreviewCollectionItem.forLikesAndStations(myCollection.getLikes(), myCollection.getStations()),
                PlaylistHeaderCollectionItem.create(PLAYLIST_COUNT),
                PlaylistCollectionItem.create(playlistItems.get(0)),
                PlaylistCollectionItem.create(playlistItems.get(1)),
                PlaylistRemoveFilterCollectionItem.create()
        );
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForOfflineOnly() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showOfflineOnly(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                PreviewCollectionItem.forLikesAndStations(myCollection.getLikes(), myCollection.getStations()),
                PlaylistHeaderCollectionItem.create(PLAYLIST_COUNT),
                PlaylistCollectionItem.create(playlistItems.get(0)),
                PlaylistCollectionItem.create(playlistItems.get(1)),
                PlaylistRemoveFilterCollectionItem.create()
        );
    }

    @Test
    public void doesNotAddFilterRemovalWhenBothFiltersApplied() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showPosts(true).showLikes(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                PreviewCollectionItem.forLikesAndStations(myCollection.getLikes(), myCollection.getStations()),
                PlaylistHeaderCollectionItem.create(PLAYLIST_COUNT),
                PlaylistCollectionItem.create(playlistItems.get(0)),
                PlaylistCollectionItem.create(playlistItems.get(1))
        );
    }

    @Test
    public void doesNotAddFilterRemovalWhenNoFiltersApplied() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                PreviewCollectionItem.forLikesAndStations(myCollection.getLikes(), myCollection.getStations()),
                PlaylistHeaderCollectionItem.create(PLAYLIST_COUNT),
                PlaylistCollectionItem.create(playlistItems.get(0)),
                PlaylistCollectionItem.create(playlistItems.get(1))
        );
    }

    @Test
    public void addsEmptyPlaylistsItemWithNoPlaylistsAndNoFilters() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                PreviewCollectionItem.forLikesAndStations(myCollection.getLikes(), myCollection.getStations()),
                PlaylistHeaderCollectionItem.create(ZERO_PLAYLIST_COUNT),
                EmptyPlaylistCollectionItem.create()
        );
    }

    @Test
    public void addsEmptyPlaylistsItemWithNoPlaylistsAndBothFilters() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                PreviewCollectionItem.forLikesAndStations(myCollection.getLikes(), myCollection.getStations()),
                PlaylistHeaderCollectionItem.create(ZERO_PLAYLIST_COUNT),
                EmptyPlaylistCollectionItem.create()
        );
    }

    @Test
    public void collectionsItemsShouldContainPreviewCollectionItemWhenThereAreNoLikesOrStations() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollection myCollection = getMyCollection(playlistItems,
                                                          LikesItem.fromTrackPreviews(Collections.<LikedTrackPreview>emptyList()),
                                                          Collections.<StationRecord>emptyList(), false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                PreviewCollectionItem.forLikesAndStations(myCollection.getLikes(), myCollection.getStations()),
                PlaylistHeaderCollectionItem.create(ZERO_PLAYLIST_COUNT),
                EmptyPlaylistCollectionItem.create()
        );
    }

    @Test
    public void collectionsItemsShouldPreviewCollectionItemWhenThereAreStations() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                PreviewCollectionItem.forLikesAndStations(myCollection.getLikes(), myCollection.getStations()),
                PlaylistHeaderCollectionItem.create(ZERO_PLAYLIST_COUNT),
                EmptyPlaylistCollectionItem.create()
        );
    }

    @NonNull
    private MyCollection getMyCollection(List<PlaylistItem> playlistItems,
                                         LikesItem likes,
                                         List<StationRecord> recentStations,
                                         boolean atLeastOneError) {
        return MyCollection.forCollectionWithPlaylists(likes, playlistItems, recentStations, atLeastOneError);
    }
}
