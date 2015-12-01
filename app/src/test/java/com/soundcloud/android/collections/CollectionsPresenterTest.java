package com.soundcloud.android.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.util.Collections;
import java.util.List;

public class CollectionsPresenterTest extends AndroidUnitTest {

    public static final List<Urn> RECENT_STATIONS = Collections.singletonList(Urn.forTrackStation(123L));

    private CollectionsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private CollectionsOperations collectionsOperations;
    @Mock private CollectionsOptionsStorage collectionsOptionsStorage;
    @Mock private CollectionsPlaylistOptionsPresenter optionsPresenter;
    @Mock private CollectionsAdapter adapter;
    @Mock private Fragment fragment;

    private TestEventBus eventBus = new TestEventBus();
    private PlaylistsOptions options;

    @Before
    public void setUp() throws Exception {
        when(collectionsOperations.collections(any(PlaylistsOptions.class))).thenReturn(Observable.<MyCollections>empty());
        when(collectionsOperations.onCollectionSynced()).thenReturn(Observable.<SyncResult>empty());
        when(collectionsOperations.onCollectionChanged()).thenReturn(Observable.<EntityStateChangedEvent>empty());
        options = PlaylistsOptions.builder().build();
        when(collectionsOptionsStorage.getLastOrDefault()).thenReturn(options);
        presenter = new CollectionsPresenter(swipeRefreshAttacher, collectionsOperations, collectionsOptionsStorage, adapter, optionsPresenter, resources(), eventBus);
    }

    @Test
    public void unsubscribesFromEventBusInOnDestroy() {
        setupDefaultCollection();
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
        final MyCollections myCollections = getMyCollection(
                ModelFixtures.create(PlaylistItem.class, 2),
                Collections.singletonList(Urn.forTrack(123L)),
                RECENT_STATIONS, false);

        when(collectionsOptionsStorage.getLastOrDefault()).thenReturn(options);
        when(collectionsOperations.collections(any(PlaylistsOptions.class))).thenReturn(Observable.just(myCollections));
        presenter.onCreate(fragment, null);

        verify(collectionsOperations).collections(options);
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForLikes() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollections myCollections = getMyCollection(playlistItems, Collections.singletonList(Urn.forTrack(123L)), RECENT_STATIONS, false);

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
        final MyCollections myCollections = getMyCollection(playlistItems, Collections.singletonList(Urn.forTrack(123L)), RECENT_STATIONS, false);

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
        final MyCollections myCollections = getMyCollection(playlistItems, Collections.singletonList(Urn.forTrack(123L)), RECENT_STATIONS, false);

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
        final MyCollections myCollections = getMyCollection(playlistItems, Collections.singletonList(Urn.forTrack(123L)), RECENT_STATIONS, false);

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
        final MyCollections myCollections = getMyCollection(playlistItems, Collections.singletonList(Urn.forTrack(123L)), RECENT_STATIONS, false);

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
        final MyCollections myCollections = getMyCollection(playlistItems, Collections.singletonList(Urn.forTrack(123L)), RECENT_STATIONS, false);

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
        final MyCollections myCollections = getMyCollection(playlistItems, Collections.<Urn>emptyList(), Collections.<Urn>emptyList(), false);

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
        final MyCollections myCollections = getMyCollection(playlistItems, Collections.<Urn>emptyList(), RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.toCollectionsItems.call(myCollections)).containsExactly(
                CollectionsItem.fromCollectionsPreview(myCollections.getLikes(), myCollections.getRecentStations()),
                CollectionsItem.fromPlaylistHeader(),
                CollectionsItem.fromEmptyPlaylists()
        );
    }

    @Test
    public void onCollectionSyncedShouldNotRefreshUntilAfterFirstLoad() {
        final PublishSubject<SyncResult> collectionSyncedBus = PublishSubject.create();
        when(collectionsOperations.onCollectionSynced()).thenReturn(collectionSyncedBus);
        when(collectionsOperations.collections(any(PlaylistsOptions.class))).thenReturn(PublishSubject.<MyCollections>create());
        presenter.onCreate(fragment, null);
        reset(collectionsOperations);

        collectionSyncedBus.onNext(SyncResult.success("syncResult", true));

        verify(collectionsOperations, never()).collections(any(PlaylistsOptions.class));
    }

    @Test
    public void onCollectionSyncedShouldRefresh() {
        setupDefaultCollection();
        final PublishSubject<SyncResult> collectionSyncedBus = PublishSubject.create();
        when(collectionsOperations.onCollectionSynced()).thenReturn(collectionSyncedBus);

        presenter.onCreate(fragment, null);
        reset(collectionsOperations);

        collectionSyncedBus.onNext(SyncResult.success("syncResult", true));

        verify(collectionsOperations).collections(any(PlaylistsOptions.class));
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
        verify(collectionsOperations, never()).collections(any(PlaylistsOptions.class));
    }

    private void setupDefaultCollection() {
        final MyCollections myCollections = getMyCollection(Collections.<PlaylistItem>emptyList(), Collections.<Urn>emptyList(), Collections.<Urn>emptyList(), false);
        when(collectionsOperations.collections(any(PlaylistsOptions.class))).thenReturn(Observable.just(myCollections));
    }

    @NonNull
    private MyCollections getMyCollection(List<PlaylistItem> playlistItems, List<Urn> likes, List<Urn> recentStations, boolean atLeastOneError) {
        return new MyCollections(likes, playlistItems, recentStations, atLeastOneError);
    }
}
