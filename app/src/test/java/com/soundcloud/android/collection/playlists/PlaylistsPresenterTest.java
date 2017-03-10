package com.soundcloud.android.collection.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.CollectionOptionsStorage;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.CollapsingScrollHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.support.v4.app.Fragment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaylistsPresenterTest extends AndroidUnitTest {

    private static final int PLAYLIST_COUNT = 2;
    private static final int ZERO_PLAYLIST_COUNT = 0;

    private static final List<Playlist> PLAYLISTS = Arrays.asList(
            ModelFixtures.playlist(),
            ModelFixtures.playlist()
    );

    private static final List<PlaylistItem> PLAYLISTS_ITEMS = Lists.transform(PLAYLISTS, ModelFixtures::playlistItem);

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private PlaylistsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private FilterHeaderPresenterFactory filterHeaderPresenterFactory;
    @Mock private MyPlaylistsOperations myPlaylistsOperations;
    @Mock private CollectionOptionsStorage collectionOptionsStorage;
    @Mock private PlaylistOptionsPresenter optionsPresenter;
    @Mock private PlaylistsAdapter adapter;
    @Mock private Fragment fragment;
    @Mock private CollapsingScrollHelper scrollHelper;
    @Mock private FilterHeaderPresenter myPlaylistHeaderPresenter;
    @Mock private OfflinePropertiesProvider offlinePropertiesProvider;
    @Mock private FeatureFlags featureFlags;

    private TestEventBus eventBus = new TestEventBus();
    private PlaylistsOptions options;

    @Before
    public void setUp() throws Exception {
        when(myPlaylistsOperations.myPlaylists(any(PlaylistsOptions.class))).thenReturn(Observable.empty());
        options = PlaylistsOptions.builder().build();
        when(collectionOptionsStorage.getLastOrDefault()).thenReturn(options);
        when(filterHeaderPresenterFactory.create(any(FilterHeaderPresenter.Listener.class), anyInt())).thenReturn(
                myPlaylistHeaderPresenter);
        presenter = new PlaylistsPresenter(
                swipeRefreshAttacher,
                myPlaylistsOperations,
                collectionOptionsStorage,
                adapter,
                optionsPresenter,
                resources(),
                eventBus,
                offlinePropertiesProvider,
                featureFlags,
                ModelFixtures.entityItemCreator());

        when(myPlaylistsOperations.myPlaylists(any(PlaylistsOptions.class))).thenReturn(Observable.just(PLAYLISTS));
    }

    @Test
    public void updatesStoredOptionsWhenOptionsUpdated() {
        final PlaylistsOptions options = PlaylistsOptions.builder().build();

        presenter.onOptionsUpdated(options);

        verify(collectionOptionsStorage).store(options);
    }

    @Test
    public void usesFilterFromStorageForInitialLoad() {
        when(collectionOptionsStorage.getLastOrDefault()).thenReturn(options);

        presenter.onCreate(fragment, null);

        verify(myPlaylistsOperations).myPlaylists(options);
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForLikes() {
        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).build());

        assertThat(presenter.playlistCollectionItems(PLAYLISTS_ITEMS)).containsExactly(
                PlaylistCollectionHeaderItem.create(PLAYLIST_COUNT),
                PlaylistCollectionPlaylistItem.create(PLAYLISTS_ITEMS.get(0)),
                PlaylistCollectionPlaylistItem.create(PLAYLISTS_ITEMS.get(1)),
                PlaylistCollectionRemoveFilterItem.create()
        );
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForPosts() {
        presenter.onOptionsUpdated(PlaylistsOptions.builder().showPosts(true).build());

        assertThat(presenter.playlistCollectionItems(PLAYLISTS_ITEMS)).containsExactly(
                PlaylistCollectionHeaderItem.create(PLAYLIST_COUNT),
                PlaylistCollectionPlaylistItem.create(PLAYLISTS_ITEMS.get(0)),
                PlaylistCollectionPlaylistItem.create(PLAYLISTS_ITEMS.get(1)),
                PlaylistCollectionRemoveFilterItem.create()
        );
    }

    @Test
    public void addsFilterRemovalWhenFilterAppliedForOfflineOnly() {
        presenter.onOptionsUpdated(PlaylistsOptions.builder().showOfflineOnly(true).build());

        assertThat(presenter.playlistCollectionItems(PLAYLISTS_ITEMS)).containsExactly(
                PlaylistCollectionHeaderItem.create(PLAYLIST_COUNT),
                PlaylistCollectionPlaylistItem.create(PLAYLISTS_ITEMS.get(0)),
                PlaylistCollectionPlaylistItem.create(PLAYLISTS_ITEMS.get(1)),
                PlaylistCollectionRemoveFilterItem.create()
        );
    }

    @Test
    public void doesNotAddFilterRemovalWhenBothFiltersApplied() {
        presenter.onOptionsUpdated(PlaylistsOptions.builder().showPosts(true).showLikes(true).build());

        assertThat(presenter.playlistCollectionItems(PLAYLISTS_ITEMS)).containsExactly(
                PlaylistCollectionHeaderItem.create(PLAYLIST_COUNT),
                PlaylistCollectionPlaylistItem.create(PLAYLISTS_ITEMS.get(0)),
                PlaylistCollectionPlaylistItem.create(PLAYLISTS_ITEMS.get(1))
        );
    }

    @Test
    public void doesNotAddFilterRemovalWhenNoFiltersApplied() {
        presenter.onOptionsUpdated(PlaylistsOptions.builder().build());

        assertThat(presenter.playlistCollectionItems(PLAYLISTS_ITEMS)).containsExactly(
                PlaylistCollectionHeaderItem.create(PLAYLIST_COUNT),
                PlaylistCollectionPlaylistItem.create(PLAYLISTS_ITEMS.get(0)),
                PlaylistCollectionPlaylistItem.create(PLAYLISTS_ITEMS.get(1))
        );
    }

    @Test
    public void addsEmptyPlaylistsItemWithNoPlaylistsAndNoFilters() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();

        presenter.onOptionsUpdated(PlaylistsOptions.builder().build());

        assertThat(presenter.playlistCollectionItems(playlistItems)).containsExactly(
                PlaylistCollectionHeaderItem.create(ZERO_PLAYLIST_COUNT),
                PlaylistCollectionEmptyPlaylistItem.create()
        );
    }

    @Test
    public void addsEmptyPlaylistsItemWithNoPlaylistsAndBothFilters() {
        final List<PlaylistItem> playlistItems = Collections.emptyList();

        presenter.onOptionsUpdated(PlaylistsOptions.builder().showLikes(true).showPosts(true).build());

        assertThat(presenter.playlistCollectionItems(playlistItems)).containsExactly(
                PlaylistCollectionHeaderItem.create(ZERO_PLAYLIST_COUNT),
                PlaylistCollectionEmptyPlaylistItem.create()
        );
    }
}
