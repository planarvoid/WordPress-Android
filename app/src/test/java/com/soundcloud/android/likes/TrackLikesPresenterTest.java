package com.soundcloud.android.likes;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.TrackLikesPresenter.DataSource;
import com.soundcloud.android.likes.TrackLikesPresenter.TrackLikesPage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.CollapsingScrollHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.support.annotation.NonNull;
import android.view.View;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TrackLikesPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);
    private final PublishSubject<OfflineProperties> offlinePropertiesSubject = PublishSubject.create();

    private TrackLikesPresenter presenter;

    @Mock private DataSource dataSource;
    @Mock private TrackLikeOperations likeOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private TrackLikesAdapterFactory adapterFactory;
    @Mock private TrackLikesAdapter adapter;
    @Mock private TrackLikesHeaderPresenter headerPresenter;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private CollapsingScrollHelper collapsingScrollHelper;
    @Mock private OfflinePropertiesProvider offlinePropertiesProvider;
    @Mock private FeatureFlags featureFlags;

    private final PublishSubject<TrackLikesPage> likedTracksObservable = PublishSubject.create();
    private final Observable<List<Urn>> likedTrackUrns = Observable.just(Arrays.asList(Urn.forTrack(1),
                                                                                       Urn.forTrack(2)));
    private TestSubscriber testSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setup() {
        when(offlinePropertiesProvider.states()).thenReturn(offlinePropertiesSubject);
        when(adapterFactory.create(headerPresenter)).thenReturn(adapter);
        presenter = new TrackLikesPresenter(likeOperations,
                                            playbackInitiator,
                                            offlineContentOperations,
                                            adapterFactory,
                                            headerPresenter,
                                            expandPlayerSubscriberProvider,
                                            eventBus,
                                            swipeRefreshAttacher,
                                            dataSource,
                                            offlinePropertiesProvider,
                                            featureFlags);
        when(dataSource.initialTrackLikes()).thenReturn(likedTracksObservable);
        when(likeOperations.likedTrackUrns()).thenReturn(likedTrackUrns);
        when(likeOperations.onTrackLiked()).thenReturn(Observable.empty());
        when(likeOperations.onTrackUnliked()).thenReturn(Observable.empty());
        when(offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges()).thenReturn(Observable.just(true));
    }

    @Test
    public void shouldRedirectOnDestroyViewToHeaderPresenter() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());
        verify(headerPresenter).onDestroyView(fragmentRule.getFragment());
    }

    @Test
    public void shouldAttachRecyclerViewToImpressionControllerInOnViewCreated() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
    }

    @Test
    public void shouldDetachRecyclerViewToImpressionControllerInOnDestroyView() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());
    }

    @Test
    public void shouldPlayLikedTracksOnListItemClick() {
        PlaybackResult playbackResult = setupPlaybackConditions(new TrackLikesTrackItem(ModelFixtures.create(TrackItem.class)));
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        testSubscriber.assertReceivedOnNext(singletonList(playbackResult));
    }

    @Test
    public void shouldNotSendUpsellEventOnMidTierItemClickWhenUserCannotUpgrade() {
        final TrackLikesTrackItem clickedTrack = new TrackLikesTrackItem(TestPropertySets.highTierTrack());
        setupPlaybackConditions(clickedTrack);

        when(adapter.getItem(0)).thenReturn(clickedTrack);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }


    @Test
    public void shouldNotPlayTracksOnListItemClickIfItemIsNull() {
        when(adapter.getItem(0)).thenReturn(null);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        verifyZeroInteractions(playbackInitiator);
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesWhenViewsAreDestroyed() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldUpdateAdapterWhenLikedTrackDownloaded() {
        when(featureFlags.isEnabled(Flag.OFFLINE_PROPERTIES_PROVIDER)).thenReturn(false);

        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        final OfflineContentChangedEvent downloadingEvent = downloading(singletonList(track.getUrn()), true);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        reset(adapter);

        final List<TrackLikesItem> trackLikesTrackItems = new ArrayList<>();
        trackLikesTrackItems.add(new TrackLikesTrackItem(TrackItem.from(track)));

        when(adapter.getItems()).thenReturn(trackLikesTrackItems);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, downloadingEvent);

        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void shouldUpdateAdapterWhenLikedTrackDownloadedWhenOfflinePropertiesProviderIsEnabled() {
        when(featureFlags.isEnabled(Flag.OFFLINE_PROPERTIES_PROVIDER)).thenReturn(true);

        final ApiTrack track = ModelFixtures.create(ApiTrack.class);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        reset(adapter);

        final List<TrackLikesItem> trackLikesTrackItems = new ArrayList<>();
        trackLikesTrackItems.add(new TrackLikesTrackItem(TrackItem.from(track)));

        when(adapter.getItems()).thenReturn(trackLikesTrackItems);
        offlinePropertiesSubject.onNext(OfflineProperties.from(singletonMap(track.getUrn(), OfflineState.DOWNLOADING), OfflineState.NOT_OFFLINE));

        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void shouldUpdateAdapterOnOfflineLikesOrOfflineContentStateChange() {
        PublishSubject<Boolean> featureChange = PublishSubject.create();
        when(offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges()).thenReturn(featureChange);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        featureChange.onNext(true);

        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void loadingTrackLikesUpdatesHeaderViewOnlyOnce() {
        List<Urn> likedTrackUrns = Arrays.asList(Urn.forTrack(1), Urn.forTrack(2));
        when(likeOperations.likedTrackUrns()).thenReturn(Observable.just(likedTrackUrns));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        likedTracksObservable.onNext(TrackLikesPage.withHeader(Collections.<LikeWithTrack>emptyList()));
        likedTracksObservable.onNext(TrackLikesPage.withHeader(Collections.<LikeWithTrack>emptyList()));

        verify(headerPresenter).updateTrackCount(likedTrackUrns.size());
    }

    @NotNull
    private PlaybackResult setupPlaybackConditions(TrackLikesTrackItem clickedTrack) {

        PlaybackResult playbackResult = PlaybackResult.success();
        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(playbackInitiator.playTracks(eq(likedTrackUrns),
                                          eq(clickedTrack.getTrackItem().getUrn()),
                                          eq(0),
                                          isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(playbackResult));
        return playbackResult;
    }

    @Test
    public void dataSourceInitialTrackLikesReturnsLikedTracksWithHeader() {
        TrackItem trackItem = ModelFixtures.trackItem();
        final List<LikeWithTrack> tracks = singletonList(getLikeWithTrack(trackItem, new Date()));

        when(likeOperations.likedTracks()).thenReturn(Observable.just(tracks));

        dataSource = new DataSource(likeOperations);
        dataSource.initialTrackLikes().subscribe(testSubscriber);

        testSubscriber.assertReceivedOnNext(singletonList(TrackLikesPage.withHeader(tracks)));
    }

    @Test
    public void dataSourceUpdatedTrackLikesReturnsLikedTracksWithHeader() {
        TrackItem trackItem = ModelFixtures.trackItem();
        final List<LikeWithTrack> tracks = singletonList(getLikeWithTrack(trackItem, new Date()));
        when(likeOperations.updatedLikedTracks()).thenReturn(Observable.just(tracks));

        new DataSource(likeOperations).updatedTrackLikes().subscribe(testSubscriber);

        testSubscriber.assertReceivedOnNext(singletonList(TrackLikesPage.withHeader(tracks)));
    }

    @Test
    public void dataSourcePagerReturnsLikedTracksWithHeader() {
        final Date oldestDate = new Date(1);
        TrackItem trackItem = ModelFixtures.trackItem();
        final List<LikeWithTrack> tracks = Arrays.asList(
                getLikeWithTrack(trackItem, oldestDate),
                getLikeWithTrack(trackItem, new Date(2))
        );

        when(likeOperations.likedTracks(oldestDate.getTime())).thenReturn(Observable.just(tracks));

        new DataSource(likeOperations).pagingFunction().call(TrackLikesPage.withHeader(tracks));

        testSubscriber.assertReceivedOnNext(emptyList());
    }

    @Test
    public void trackPagerFinishesIfLastPageIncomplete() throws Exception {
        TrackItem trackItem = ModelFixtures.trackItem();
        final List<LikeWithTrack> tracks = singletonList(getLikeWithTrack(trackItem, new Date()));

        final Pager.PagingFunction<TrackLikesPage> listPager = new DataSource(likeOperations).pagingFunction();

        assertThat(listPager.call(TrackLikesPage.withHeader(tracks))).isSameAs(Pager.<List<PropertySet>>finish());
    }

    @NonNull
    LikeWithTrack getLikeWithTrack(TrackItem trackItem, Date likedAt) {
        return LikeWithTrack.create(Like.create(trackItem.getUrn(), likedAt), trackItem);
    }
}
