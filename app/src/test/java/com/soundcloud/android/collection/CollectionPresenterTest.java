package com.soundcloud.android.collection;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.playhistory.PlayHistoryBucketItem;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.playlists.PlaylistOptionsPresenter;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketItem;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.support.v4.app.Fragment;

import javax.inject.Provider;
import java.util.Collections;
import java.util.List;

public class CollectionPresenterTest extends AndroidUnitTest {

    private static final LikesItem LIKES = LikesItem.fromTrackPreviews(singletonList(
            LikedTrackPreview.create(Urn.forTrack(123L), "http://image-url")));
    private static final LikesItem NO_LIKES = LikesItem.fromTrackPreviews(Collections.emptyList());

    private static final List<PlaylistItem> PLAYLISTS = ModelFixtures.playlistItem(2);
    private static final List<StationRecord> STATIONS = singletonList(mock(StationRecord.class));
    private static final List<TrackItem> PLAY_HISTORY = singletonList(mock(TrackItem.class));
    private static final List<RecentlyPlayedPlayableItem> RECENTLY_PLAYED = singletonList(mock(
            RecentlyPlayedPlayableItem.class));

    private static final MyCollection MY_COLLECTION = MyCollection.forCollectionWithPlayHistory(LIKES,
                                                                                                PLAYLISTS,
                                                                                                STATIONS,
                                                                                                PLAY_HISTORY,
                                                                                                RECENTLY_PLAYED,
                                                                                                false);
    private static final MyCollection MY_COLLECTION_WITHOUT_PLAY_HISTORY = MyCollection.forCollectionWithPlayHistory(
            LIKES,
            PLAYLISTS,
            STATIONS,
            Collections.emptyList(),
            Collections.emptyList(),
            false);
    private static final MyCollection MY_COLLECTION_EMPTY = MyCollection.forCollectionWithPlayHistory(NO_LIKES,
                                                                                                      Collections.emptyList(),
                                                                                                      Collections.emptyList(),
                                                                                                      Collections.emptyList(),
                                                                                                      Collections.emptyList(),
                                                                                                      false);

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private CollectionPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private CollectionOperations collectionOperations;
    @Mock private CollectionOptionsStorage collectionOptionsStorage;
    @Mock private PlaylistOptionsPresenter optionsPresenter;
    @Mock private CollectionAdapter adapter;
    @Mock private Fragment fragment;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;
    @Mock private PlayHistoryOperations playHistoryOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private Navigator navigator;
    @Mock private OfflinePropertiesProvider offlinePropertiesProvider;
    @Mock private FeatureFlags featureFlags;

    private Provider expandPlayerSubscriberProvider = providerOf(expandPlayerSubscriber);
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        when(collectionOperations.collections()).thenReturn(Observable.just(MY_COLLECTION));
        when(collectionOperations.onCollectionChanged()).thenReturn(Observable.empty());
        when(RECENTLY_PLAYED.get(0).getUrn()).thenReturn(Urn.forPlaylist(123L));
        presenter = new CollectionPresenter(swipeRefreshAttacher,
                                            collectionOperations,
                                            collectionOptionsStorage,
                                            adapter,
                                            resources(),
                                            eventBus,
                                            expandPlayerSubscriberProvider,
                                            playHistoryOperations,
                                            featureOperations,
                                            navigator,
                                            offlinePropertiesProvider,
                                            featureFlags);
    }

    @Test
    public void shouldPresentPreviewOfLikesAndPlaylistsWithPlayHistoryAndRecentlyPlayed() {
        Iterable<CollectionItem> collectionItems = presenter.toCollectionItems.call(MY_COLLECTION);

        assertThat(collectionItems).containsExactly(
                PreviewCollectionItem.forLikesPlaylistsAndStations(MY_COLLECTION.getLikes(),
                                                                   MY_COLLECTION.getPlaylistItems(),
                                                                   MY_COLLECTION.getStations()),
                RecentlyPlayedBucketItem.create(RECENTLY_PLAYED),
                PlayHistoryBucketItem.create(PLAY_HISTORY)
        );
    }

    @Test
    public void shouldPresentPreviewWhenNoPlayHistory() {
        MyCollection myCollection = MY_COLLECTION_WITHOUT_PLAY_HISTORY;
        Iterable<CollectionItem> collectionItems = presenter.toCollectionItems.call(myCollection);

        assertThat(collectionItems).containsExactly(
                PreviewCollectionItem.forLikesPlaylistsAndStations(myCollection.getLikes(),
                                                                   myCollection.getPlaylistItems(),
                                                                   myCollection.getStations()),
                RecentlyPlayedBucketItem.create(Collections.emptyList()),
                PlayHistoryBucketItem.create(Collections.emptyList())
        );
    }

    @Test
    public void shouldPresentPreviewWhenNoLikesOrPlaylists() {
        MyCollection myCollection = MY_COLLECTION_EMPTY;
        Iterable<CollectionItem> collectionItems = presenter.toCollectionItems.call(myCollection);

        assertThat(collectionItems).containsExactly(
                PreviewCollectionItem.forLikesPlaylistsAndStations(myCollection.getLikes(),
                                                                   myCollection.getPlaylistItems(),
                                                                   myCollection.getStations()),
                RecentlyPlayedBucketItem.create(Collections.emptyList()),
                PlayHistoryBucketItem.create(Collections.emptyList())
        );
    }

    @Test
    public void onCollectionChangedShouldNotRefreshUntilAfterFirstLoad() {
        final PublishSubject<Object> collectionSyncedBus = PublishSubject.create();
        when(collectionOperations.onCollectionChanged()).thenReturn(collectionSyncedBus);
        when(collectionOperations.collections()).thenReturn(PublishSubject.create());
        presenter.onCreate(fragment, null);
        reset(collectionOperations);

        collectionSyncedBus.onNext(null);

        verify(collectionOperations, never()).collections();
    }

    @Test
    public void onCollectionChangedShouldRefresh() {
        final PublishSubject<Object> collectionSyncedBus = PublishSubject.create();
        when(collectionOperations.onCollectionChanged()).thenReturn(collectionSyncedBus);

        presenter.onCreate(fragment, null);
        reset(collectionOperations);

        collectionSyncedBus.onNext(null);

        verify(collectionOperations).collections();
    }

    @Test
    public void onCollectionChangedShouldNotRefreshWhenAlreadyRefreshing() {
        final PublishSubject<Object> collectionSyncedBus = PublishSubject.create();
        when(collectionOperations.onCollectionChanged()).thenReturn(collectionSyncedBus);
        when(collectionOperations.collections()).thenReturn(PublishSubject.create());
        when(swipeRefreshAttacher.isRefreshing()).thenReturn(true);
        presenter.onCreate(fragment, null);
        reset(collectionOperations);

        collectionSyncedBus.onNext(null);
        verify(collectionOperations, never()).collections();
    }

    @Test
    public void shouldSetListeners() {
        verify(adapter).setTrackClickListener(presenter);
        verify(adapter).setOnboardingListener(presenter);
        verify(adapter).setUpsellListener(presenter);
    }

    @Test
    public void shouldAddOnboardingWhenEnabled() {
        when(collectionOptionsStorage.isOnboardingEnabled()).thenReturn(true);
        when(collectionOptionsStorage.isUpsellEnabled()).thenReturn(true);

        presenter.onCreate(fragment, null);

        assertThat(presenter.toCollectionItems.call(MY_COLLECTION)).containsExactly(
                CollectionItem.OnboardingCollectionItem.create(),
                PreviewCollectionItem.forLikesPlaylistsAndStations(
                        MY_COLLECTION.getLikes(), MY_COLLECTION.getPlaylistItems(), MY_COLLECTION.getStations()),
                RecentlyPlayedBucketItem.create(RECENTLY_PLAYED),
                PlayHistoryBucketItem.create(PLAY_HISTORY)
        );
    }

    @Test
    public void shouldDisableOnboardingWhenClosed() {
        presenter.onCollectionsOnboardingItemClosed(0);

        verify(collectionOptionsStorage).disableOnboarding();
    }

    @Test
    public void shouldRemoveOnboardingWhenClosed() {
        presenter.onCollectionsOnboardingItemClosed(1);

        verify(adapter).removeItem(1);
    }

    @Test
    public void shouldAddUpsellWhenEnabledAndOnboardingDisabled() {
        when(collectionOptionsStorage.isUpsellEnabled()).thenReturn(true);
        when(collectionOptionsStorage.isOnboardingEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        presenter.onCreate(fragment, null);

        assertThat(presenter.toCollectionItems.call(MY_COLLECTION)).containsExactly(
                CollectionItem.UpsellCollectionItem.create(),
                PreviewCollectionItem.forLikesPlaylistsAndStations(
                        MY_COLLECTION.getLikes(), MY_COLLECTION.getPlaylistItems(), MY_COLLECTION.getStations()),
                RecentlyPlayedBucketItem.create(RECENTLY_PLAYED),
                PlayHistoryBucketItem.create(PLAY_HISTORY)
        );
    }

    @Test
    public void shouldNotAddUpsellWhenUpgradeIsUnavailable() {
        when(collectionOptionsStorage.isUpsellEnabled()).thenReturn(true);
        when(collectionOptionsStorage.isOnboardingEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);

        presenter.onCreate(fragment, null);

        assertThat(presenter.toCollectionItems.call(MY_COLLECTION)).containsExactly(
                PreviewCollectionItem.forLikesPlaylistsAndStations(
                        MY_COLLECTION.getLikes(), MY_COLLECTION.getPlaylistItems(), MY_COLLECTION.getStations()),
                RecentlyPlayedBucketItem.create(RECENTLY_PLAYED),
                PlayHistoryBucketItem.create(PLAY_HISTORY)
        );
    }

    @Test
    public void shouldEmitImpressionEventWhenUpsellAdded() {
        when(collectionOptionsStorage.isUpsellEnabled()).thenReturn(true);
        when(collectionOptionsStorage.isOnboardingEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(true);

        presenter.onCreate(fragment, null);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION.toString());
    }

    @Test
    public void shouldDisableUpsellWhenClosed() {
        presenter.onUpsellClose(0);

        verify(collectionOptionsStorage).disableUpsell();
    }

    @Test
    public void shouldRemoveUpsellWhenClosed() {
        presenter.onUpsellClose(1);

        verify(adapter).removeItem(1);
    }

    @Test
    public void shouldNavigateToConversionPageOnUpsellClick() {
        presenter.onUpsell(context());

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK.toString());
        verify(navigator).openUpgrade(context());
    }
}
