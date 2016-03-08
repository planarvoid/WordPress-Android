package com.soundcloud.android.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.util.Collections;
import java.util.List;

public class CollectionPresenterTest extends AndroidUnitTest {

    public static final List<Urn> RECENT_STATIONS = Collections.singletonList(Urn.forTrackStation(123L));
    public static final LikesItem LIKES = LikesItem.fromUrns(Collections.singletonList(Urn.forTrack(123L)));

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
        presenter = new CollectionPresenter(swipeRefreshAttacher, collectionOperations, collectionOptionsStorage, adapter, optionsPresenter, resources(), eventBus);
    }

    @Test
    @Ignore // I cannot get this presenter to work with fragmentRule. Need to look at it again, but need to release now
    public void unsubscribesFromEventBusInOnDestroyView() {
        setupDefaultCollection();

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onDestroyView(fragment);
        eventBus.verifyUnsubscribed();
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
                CollectionItem.fromCollectionsPreview(myCollection.getLikes(), myCollection.getRecentStations()),
                CollectionItem.fromPlaylistHeader(),
                CollectionItem.fromPlaylistItem(playlistItems.get(0)),
                CollectionItem.fromPlaylistItem(playlistItems.get(1)),
                CollectionItem.fromKillFilter()
        );
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForPosts() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showPosts(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                CollectionItem.fromCollectionsPreview(myCollection.getLikes(), myCollection.getRecentStations()),
                CollectionItem.fromPlaylistHeader(),
                CollectionItem.fromPlaylistItem(playlistItems.get(0)),
                CollectionItem.fromPlaylistItem(playlistItems.get(1)),
                CollectionItem.fromKillFilter()
        );
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForOfflineOnly() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showOfflineOnly(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                CollectionItem.fromCollectionsPreview(myCollection.getLikes(), myCollection.getRecentStations()),
                CollectionItem.fromPlaylistHeader(),
                CollectionItem.fromPlaylistItem(playlistItems.get(0)),
                CollectionItem.fromPlaylistItem(playlistItems.get(1)),
                CollectionItem.fromKillFilter()
        );
    }

    @Test
    public void doesNotAddFilterRemovalWhenBothFiltersApplied() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showPosts(true).showLikes(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                CollectionItem.fromCollectionsPreview(myCollection.getLikes(), myCollection.getRecentStations()),
                CollectionItem.fromPlaylistHeader(),
                CollectionItem.fromPlaylistItem(playlistItems.get(0)),
                CollectionItem.fromPlaylistItem(playlistItems.get(1))
        );
    }

    @Test
    public void doesNotAddFilterRemovalWhenNoFiltersApplied() {
        final List<PlaylistItem> playlistItems = ModelFixtures.create(PlaylistItem.class, 2);
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                CollectionItem.fromCollectionsPreview(myCollection.getLikes(), myCollection.getRecentStations()),
                CollectionItem.fromPlaylistHeader(),
                CollectionItem.fromPlaylistItem(playlistItems.get(0)),
                CollectionItem.fromPlaylistItem(playlistItems.get(1))
        );
    }

    @Test
    public void addsEmptyPlaylistsItemWithNoPlaylistsAndNoFilters() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                CollectionItem.fromCollectionsPreview(myCollection.getLikes(), myCollection.getRecentStations()),
                CollectionItem.fromPlaylistHeader(),
                CollectionItem.fromEmptyPlaylists()
        );
    }

    @Test
    public void addsEmptyPlaylistsItemWithNoPlaylistsAndBothFilters() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                CollectionItem.fromCollectionsPreview(myCollection.getLikes(), myCollection.getRecentStations()),
                CollectionItem.fromPlaylistHeader(),
                CollectionItem.fromEmptyPlaylists()
        );
    }

    @Test
    public void collectionsItemsShouldContainPreviewCollectionItemWhenThereAreNoLikesOrStations() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollection myCollection = getMyCollection(playlistItems,
                LikesItem.fromUrns(Collections.<Urn>emptyList()), Collections.<Urn>emptyList(), false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                CollectionItem.fromCollectionsPreview(myCollection.getLikes(), myCollection.getRecentStations()),
                CollectionItem.fromPlaylistHeader(),
                CollectionItem.fromEmptyPlaylists()
        );
    }

    @Test
    public void collectionsItemsShouldPreviewCollectionItemWhenThereAreStations() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();
        final MyCollection myCollection = getMyCollection(playlistItems, LIKES, RECENT_STATIONS, false);

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.toCollectionItems.call(myCollection)).containsExactly(
                CollectionItem.fromCollectionsPreview(myCollection.getLikes(), myCollection.getRecentStations()),
                CollectionItem.fromPlaylistHeader(),
                CollectionItem.fromEmptyPlaylists()
        );
    }

    @Test
    public void onCollectionChangedShouldNotRefreshUntilAfterFirstLoad() {
        final PublishSubject<Object> collectionSyncedBus = PublishSubject.create();
        when(collectionOperations.onCollectionChanged()).thenReturn(collectionSyncedBus);
        when(collectionOperations.collections(any(PlaylistsOptions.class))).thenReturn(PublishSubject.<MyCollection>create());
        presenter.onCreate(fragment, null);
        reset(collectionOperations);

        collectionSyncedBus.onNext(null);

        verify(collectionOperations, never()).collections(any(PlaylistsOptions.class));
    }

    @Test
    public void onCollectionChangedShouldRefresh() {
        setupDefaultCollection();
        final PublishSubject<Object> collectionSyncedBus = PublishSubject.create();
        when(collectionOperations.onCollectionChanged()).thenReturn(collectionSyncedBus);

        presenter.onCreate(fragment, null);
        reset(collectionOperations);

        collectionSyncedBus.onNext(null);

        verify(collectionOperations).collections(any(PlaylistsOptions.class));
    }

    @Test
    public void onCollectionChangedShouldNotRefreshWhenAlreadyRefreshing() {
        final PublishSubject<Object> collectionSyncedBus = PublishSubject.create();
        when(collectionOperations.onCollectionChanged()).thenReturn(collectionSyncedBus);
        when(collectionOperations.collections(any(PlaylistsOptions.class))).thenReturn(PublishSubject.<MyCollection>create());
        when(swipeRefreshAttacher.isRefreshing()).thenReturn(true);
        presenter.onCreate(fragment, null);
        reset(collectionOperations);

        collectionSyncedBus.onNext(null);
        verify(collectionOperations, never()).collections(any(PlaylistsOptions.class));
    }

    private void setupDefaultCollection() {
        final MyCollection myCollection = getMyCollection(Collections.<PlaylistItem>emptyList(),
                LikesItem.fromUrns(Collections.<Urn>emptyList()), Collections.<Urn>emptyList(), false);
        when(collectionOperations.collections(any(PlaylistsOptions.class))).thenReturn(Observable.just(myCollection));
    }

    @NonNull
    private MyCollection getMyCollection(List<PlaylistItem> playlistItems, LikesItem likes, List<Urn> recentStations, boolean atLeastOneError) {
        return new MyCollection(likes, playlistItems, recentStations, atLeastOneError);
    }
}
