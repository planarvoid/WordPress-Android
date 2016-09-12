package com.soundcloud.android.collection;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.playhistory.PlayHistoryBucketItem;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.playlists.PlaylistOptionsPresenter;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketItem;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.model.Urn;
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
    private static final LikesItem NO_LIKES = LikesItem.fromTrackPreviews(Collections.<LikedTrackPreview>emptyList());

    private static final List<PlaylistItem> PLAYLISTS = ModelFixtures.create(PlaylistItem.class, 2);
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
            Collections.<TrackItem>emptyList(),
            Collections.<RecentlyPlayedPlayableItem>emptyList(),
            false);
    private static final MyCollection MY_COLLECTION_EMPTY = MyCollection.forCollectionWithPlayHistory(NO_LIKES,
                                                                                                      Collections.<PlaylistItem>emptyList(),
                                                                                                      Collections.<StationRecord>emptyList(),
                                                                                                      Collections.<TrackItem>emptyList(),
                                                                                                      Collections.<RecentlyPlayedPlayableItem>emptyList(),
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
                                            playHistoryOperations, featureFlags);
    }

    @Test
    public void shouldPresentPreviewOfLikesAndPlaylistsWithPlayHistoryAndRecentlyPlayed() {
        Iterable<CollectionItem> collectionItems = presenter.toCollectionItems.call(MY_COLLECTION);

        assertThat(collectionItems).containsExactly(
                PreviewCollectionItem.forLikesAndPlaylists(MY_COLLECTION.getLikes(), MY_COLLECTION.getPlaylistItems()),
                RecentlyPlayedBucketItem.create(RECENTLY_PLAYED),
                PlayHistoryBucketItem.create(PLAY_HISTORY)
        );
    }

    @Test
    public void shouldPresentPreviewWhenNoPlayHistory() {
        MyCollection myCollection = MY_COLLECTION_WITHOUT_PLAY_HISTORY;
        Iterable<CollectionItem> collectionItems = presenter.toCollectionItems.call(myCollection);

        assertThat(collectionItems).containsExactly(
                PreviewCollectionItem.forLikesAndPlaylists(myCollection.getLikes(), myCollection.getPlaylistItems()),
                RecentlyPlayedBucketItem.create(Collections.<RecentlyPlayedPlayableItem>emptyList()),
                PlayHistoryBucketItem.create(Collections.<TrackItem>emptyList())
        );
    }

    @Test
    public void shouldPresentPreviewWhenNoLikesOrPlaylists() {
        MyCollection myCollection = MY_COLLECTION_EMPTY;
        Iterable<CollectionItem> collectionItems = presenter.toCollectionItems.call(myCollection);

        assertThat(collectionItems).containsExactly(
                PreviewCollectionItem.forLikesAndPlaylists(myCollection.getLikes(), myCollection.getPlaylistItems()),
                RecentlyPlayedBucketItem.create(Collections.<RecentlyPlayedPlayableItem>emptyList()),
                PlayHistoryBucketItem.create(Collections.<TrackItem>emptyList())
        );
    }

    @Test
    public void onCollectionChangedShouldNotRefreshUntilAfterFirstLoad() {
        final PublishSubject<Object> collectionSyncedBus = PublishSubject.create();
        when(collectionOperations.onCollectionChanged()).thenReturn(collectionSyncedBus);
        when(collectionOperations.collections()).thenReturn(PublishSubject.<MyCollection>create());
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
        when(collectionOperations.collections()).thenReturn(PublishSubject.<MyCollection>create());
        when(swipeRefreshAttacher.isRefreshing()).thenReturn(true);
        presenter.onCreate(fragment, null);
        reset(collectionOperations);

        collectionSyncedBus.onNext(null);
        verify(collectionOperations, never()).collections();
    }

    @Test
    public void shouldSetTrackClickListeners() {
        verify(adapter).setTrackClickListener(presenter);
    }

    @Test
    public void shouldAddOnboardingWhenEnabled() {
        when(collectionOptionsStorage.isOnboardingEnabled()).thenReturn(true);

        presenter.onCreate(fragment, null);

        assertThat(presenter.toCollectionItems.call(MY_COLLECTION)).containsExactly(
                OnboardingCollectionItem.create(),
                PreviewCollectionItem.forLikesAndPlaylists(MY_COLLECTION.getLikes(), MY_COLLECTION.getPlaylistItems()),
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

}
