package com.soundcloud.android.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.PlaylistLikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.support.v4.app.Fragment;

import java.util.Collections;
import java.util.List;

public class CollectionsPresenterTest extends AndroidUnitTest {

    public static final List<Urn> RECENT_STATIONS = Collections.singletonList(Urn.forTrackStation(123L));

    private CollectionsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private CollectionsOperations collectionsOperations;
    @Mock private PlaylistLikeOperations likeOperations;
    @Mock private CollectionsOptionsStorage collectionsOptionsStorage;
    @Mock private CollectionsPlaylistOptionsPresenter optionsPresenter;
    @Mock private CollectionsAdapter adapter;
    @Mock private Fragment fragment;
    @Mock private FeatureFlags featureFlags;

    private TestEventBus eventBus = new TestEventBus();
    private PlaylistsOptions options;

    @Before
    public void setUp() throws Exception {
        when(collectionsOperations.collections(any(PlaylistsOptions.class))).thenReturn(Observable.<MyCollections>empty());
        when(likeOperations.onPlaylistLiked()).thenReturn(Observable.<PropertySet>empty());
        when(likeOperations.onPlaylistUnliked()).thenReturn(Observable.<Urn>empty());
        when(collectionsOperations.onCollectionSynced()).thenReturn(Observable.<SyncResult>empty());
        options = PlaylistsOptions.builder().build();
        when(collectionsOptionsStorage.getLastOrDefault()).thenReturn(options);
        presenter = new CollectionsPresenter(swipeRefreshAttacher, collectionsOperations, likeOperations, collectionsOptionsStorage, adapter, optionsPresenter, resources(), eventBus);
    }

    @Test
    public void unsubscribesFromEventBusInOnDestroy() {
        presenter.onCreate(fragment, null);
        presenter.onDestroy(fragment);
        eventBus.verifyUnsubscribed();
    }

    @Test
    public void updatesStoredOptionsWhenOptionsUpdated() {
        final PlaylistsOptions options = PlaylistsOptions.builder().build();
        presenter.onOptionsUpdated(options);
        verify(collectionsOptionsStorage).store(options);
    }

    @Test
    public void usesFilterFromStorageForInitialLoad() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollections myCollections = new MyCollections(
                Collections.singletonList(Urn.forTrack(123L)),
                playlistItems,
                RECENT_STATIONS,
                false);

        when(collectionsOptionsStorage.getLastOrDefault()).thenReturn(options);
        when(collectionsOperations.collections(any(PlaylistsOptions.class))).thenReturn(Observable.just(myCollections));
        presenter.onCreate(fragment, null);

        verify(collectionsOperations).collections(options);
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForLikes() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollections myCollections = new MyCollections(
                Collections.singletonList(Urn.forTrack(123L)),
                playlistItems,
                RECENT_STATIONS,
                false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).build());

        assertThat(presenter.toCollectionsItems.call(myCollections)).containsExactly(
                CollectionsItem.fromCollectionsPreview(myCollections.getLikes(), myCollections.getRecentStations()),
                CollectionsItem.fromPlaylistHeader(),
                CollectionsItem.fromPlaylistItem(playlistItems.get(0)),
                CollectionsItem.fromPlaylistItem(playlistItems.get(1)),
                CollectionsItem.fromKillFilter()
        );
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForPosts() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollections myCollections = new MyCollections(
                Collections.singletonList(Urn.forTrack(123L)),
                playlistItems,
                RECENT_STATIONS,
                false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showPosts(true).build());

        assertThat(presenter.toCollectionsItems.call(myCollections)).containsExactly(
                CollectionsItem.fromCollectionsPreview(myCollections.getLikes(), myCollections.getRecentStations()),
                CollectionsItem.fromPlaylistHeader(),
                CollectionsItem.fromPlaylistItem(playlistItems.get(0)),
                CollectionsItem.fromPlaylistItem(playlistItems.get(1)),
                CollectionsItem.fromKillFilter()
        );
    }

    @Test
    public void doesNotAddFilterRemovalWhenBothFiltersApplied() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollections myCollections = new MyCollections(
                Collections.singletonList(Urn.forTrack(123L)),
                playlistItems, RECENT_STATIONS,
                false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showPosts(true).showLikes(true).build());

        assertThat(presenter.toCollectionsItems.call(myCollections)).containsExactly(
                CollectionsItem.fromCollectionsPreview(myCollections.getLikes(), myCollections.getRecentStations()),
                CollectionsItem.fromPlaylistHeader(),
                CollectionsItem.fromPlaylistItem(playlistItems.get(0)),
                CollectionsItem.fromPlaylistItem(playlistItems.get(1))
        );
    }

    @Test
    public void doesNotAddFilterRemovalWhenNoFiltersApplied() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollections myCollections = new MyCollections(
                Collections.singletonList(Urn.forTrack(123L)),
                playlistItems, RECENT_STATIONS,
                false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().build());

        assertThat(presenter.toCollectionsItems.call(myCollections)).containsExactly(
                CollectionsItem.fromCollectionsPreview(myCollections.getLikes(), myCollections.getRecentStations()),
                CollectionsItem.fromPlaylistHeader(),
                CollectionsItem.fromPlaylistItem(playlistItems.get(0)),
                CollectionsItem.fromPlaylistItem(playlistItems.get(1))
        );
    }

    @Test
    public void addsEmptyPlaylistsItemWithNoPlaylistsAndNoFilters() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollections myCollections = new MyCollections(
                Collections.singletonList(Urn.forTrack(123L)),
                playlistItems, RECENT_STATIONS,
                false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().build());

        assertThat(presenter.toCollectionsItems.call(myCollections)).containsExactly(
                CollectionsItem.fromCollectionsPreview(myCollections.getLikes(), myCollections.getRecentStations()),
                CollectionsItem.fromPlaylistHeader(),
                CollectionsItem.fromEmptyPlaylists()
        );
    }

    @Test
    public void addsEmptyPlaylistsItemWithNoPlaylistsAndBothFilters() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollections myCollections = new MyCollections(
                Collections.singletonList(Urn.forTrack(123L)),
                playlistItems, RECENT_STATIONS,
                false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.toCollectionsItems.call(myCollections)).containsExactly(
                CollectionsItem.fromCollectionsPreview(myCollections.getLikes(), myCollections.getRecentStations()),
                CollectionsItem.fromPlaylistHeader(),
                CollectionsItem.fromEmptyPlaylists()
        );
    }

    @Test
      public void collectionsItemsShouldContainPreviewCollectionItemWhenThereAreNoLikesOrStations() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollections myCollections = new MyCollections(
                Collections.<Urn>emptyList(),
                playlistItems,
                Collections.<Urn>emptyList(),
                false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.toCollectionsItems.call(myCollections)).containsExactly(
                CollectionsItem.fromCollectionsPreview(myCollections.getLikes(), myCollections.getRecentStations()),
                CollectionsItem.fromPlaylistHeader(),
                CollectionsItem.fromEmptyPlaylists()
        );
    }

    @Test
    public void collectionsItemsShouldPreviewCollectionItemWhenThereAreStations() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollections myCollections = new MyCollections(
                Collections.<Urn>emptyList(),
                playlistItems,
                RECENT_STATIONS,
                false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.toCollectionsItems.call(myCollections)).containsExactly(
                CollectionsItem.fromCollectionsPreview(myCollections.getLikes(), myCollections.getRecentStations()),
                CollectionsItem.fromPlaylistHeader(),
                CollectionsItem.fromEmptyPlaylists()
        );
    }

    @Test
    public void onCollectionSyncedShouldRefresh() {
        final PublishSubject<SyncResult> collectionSyncedBus = PublishSubject.create();
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollections myCollections = new MyCollections(
                Collections.singletonList(Urn.forTrack(123L)),
                playlistItems,
                RECENT_STATIONS,
                false);

        when(collectionsOperations.onCollectionSynced()).thenReturn(collectionSyncedBus);
        when(collectionsOperations.collections(any(PlaylistsOptions.class))).thenReturn(Observable.just(myCollections));
        presenter.onCreate(fragment, null);
        reset(collectionsOperations);

        collectionSyncedBus.onNext(SyncResult.success("syncResult", true));

        verify(adapter).onNext(presenter.toCollectionsItems.call(myCollections));
    }

    @Test
    public void onCollectionSyncedShouldNotRefreshWhenAlreadyRefreshing() {
        final PublishSubject<SyncResult> collectionSyncedBus = PublishSubject.create();
        when(collectionsOperations.onCollectionSynced()).thenReturn(collectionSyncedBus);
        when(collectionsOperations.collections(any(PlaylistsOptions.class))).thenReturn(PublishSubject.<MyCollections>create());
        when(swipeRefreshAttacher.isRefreshing()).thenReturn(true);
        presenter.onCreate(fragment, null);
        reset(collectionsOperations);

        collectionSyncedBus.onNext(SyncResult.success("syncResult", true));
    
        verify(adapter, never()).onNext(anyCollection());
    }
}
