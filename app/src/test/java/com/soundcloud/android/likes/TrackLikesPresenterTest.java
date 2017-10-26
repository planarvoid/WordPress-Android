package com.soundcloud.android.likes;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.TrackLikesPresenter.DataSource;
import com.soundcloud.android.likes.TrackLikesPresenter.TrackLikesPage;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.SingleSubject;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

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
    @Mock private OfflinePropertiesProvider offlinePropertiesProvider;
    @Mock private FeatureFlags featureFlags;
    @Mock private TrackLikesIntentResolver intentResolver;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Mock private ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    @Mock private ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    private final SingleSubject<TrackLikesPage> likedTracksObservable = SingleSubject.create();
    private final Single<List<Urn>> likedTrackUrns = Single.just(Arrays.asList(Urn.forTrack(1),
                                                                               Urn.forTrack(2)));
    private TestObserver testSubscriber = new TestObserver<>();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);
    private TestEventBusV2 eventBus = new TestEventBusV2();

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
                                            intentResolver,
                                            dataSource,
                                            offlinePropertiesProvider,
                                            featureFlags,
                                            performanceMetricsEngine,
                                            changeLikeToSaveExperiment,
                                            changeLikeToSaveExperimentStringHelper);
        when(dataSource.initialTrackLikes()).thenReturn(likedTracksObservable);
        when(likeOperations.likedTrackUrns()).thenReturn(likedTrackUrns);
        when(likeOperations.onTrackLiked()).thenReturn(Observable.empty());
        when(likeOperations.onTrackUnliked()).thenReturn(Observable.empty());
        when(offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges()).thenReturn(Observable.just(true));
        when(intentResolver.consumePlaybackRequest()).thenReturn(false);
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
        PlaybackResult playbackResult = setupPlaybackConditions(TrackLikesTrackItem.create(TrackFixtures.trackItem()));
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        testSubscriber.assertValue(playbackResult);
    }

    @Test
    public void shouldNotSendUpsellEventOnMidTierItemClickWhenUserCannotUpgrade() {
        final TrackLikesTrackItem clickedTrack = TrackLikesTrackItem.create(PlayableFixtures.highTierTrack());
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
        final ApiTrack track = TrackFixtures.apiTrack();

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        reset(adapter);

        final List<TrackLikesItem> trackLikesTrackItems = new ArrayList<>();
        trackLikesTrackItems.add(TrackLikesTrackItem.create(TrackFixtures.trackItem(track)));

        when(adapter.getItems()).thenReturn(trackLikesTrackItems);
        offlinePropertiesSubject.onNext(new OfflineProperties(singletonMap(track.getUrn(), OfflineState.DOWNLOADING), OfflineState.NOT_OFFLINE));

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
        when(likeOperations.likedTrackUrns()).thenReturn(Single.just(likedTrackUrns));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        likedTracksObservable.onSuccess(TrackLikesPage.withHeader(Collections.emptyList()));

        verify(headerPresenter).updateTrackCount(likedTrackUrns.size());
    }

    @NotNull
    private PlaybackResult setupPlaybackConditions(TrackLikesTrackItem clickedTrack) {

        PlaybackResult playbackResult = PlaybackResult.success();
        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(playbackInitiator.playTracks(any(Single.class),
                                          eq(clickedTrack.getTrackItem().getUrn()),
                                          eq(0),
                                          isA(PlaySessionSource.class)))
                .thenReturn(Single.just(playbackResult));
        return playbackResult;
    }

    @Test
    public void dataSourceInitialTrackLikesReturnsLikedTracksWithHeader() {
        TrackItem trackItem = TrackFixtures.trackItem();
        final List<LikeWithTrack> tracks = singletonList(getLikeWithTrack(trackItem, new Date()));

        when(likeOperations.likedTracks()).thenReturn(Single.just(tracks));

        dataSource = new DataSource(likeOperations);
        final TestObserver<TrackLikesPage> observer = dataSource.initialTrackLikes().test();

        observer.assertValue(TrackLikesPage.withHeader(tracks));
    }

    @Test
    public void dataSourceUpdatedTrackLikesReturnsLikedTracksWithHeader() {
        TrackItem trackItem = TrackFixtures.trackItem();
        final List<LikeWithTrack> tracks = singletonList(getLikeWithTrack(trackItem, new Date()));
        when(likeOperations.updatedLikedTracks()).thenReturn(Single.just(tracks));

        new DataSource(likeOperations).updatedTrackLikes().subscribe(testSubscriber);

        testSubscriber.assertValue(TrackLikesPage.withHeader(tracks));
    }

    @Test
    public void dataSourcePagerReturnsLikedTracksWithHeader() {
        final Date oldestDate = new Date(1);
        TrackItem trackItem = TrackFixtures.trackItem();
        final List<LikeWithTrack> tracks = Arrays.asList(
                getLikeWithTrack(trackItem, oldestDate),
                getLikeWithTrack(trackItem, new Date(2))
        );

        when(likeOperations.likedTracks(oldestDate.getTime())).thenReturn(Single.just(tracks));

        new DataSource(likeOperations).pagingFunction().call(TrackLikesPage.withHeader(emptyList()));

        testSubscriber.assertNoValues();
    }

    @Test
    public void trackPagerFinishesIfLastPageEmpty() throws Exception {
        final Pager.PagingFunction<TrackLikesPage> listPager = new DataSource(likeOperations).pagingFunction();

        assertThat(listPager.call(TrackLikesPage.withHeader(emptyList()))).isSameAs(Pager.finish());
    }

    @Test
    public void shouldEndMeasuringFirstPageTimeLoad() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        likedTracksObservable.onSuccess(TrackLikesPage.withHeader(Collections.emptyList()));

        verify(performanceMetricsEngine).endMeasuring(MetricType.LIKED_TRACKS_FIRST_PAGE_LOAD);
    }

    @NonNull
    LikeWithTrack getLikeWithTrack(TrackItem trackItem, Date likedAt) {
        return LikeWithTrack.create(new Association(trackItem.getUrn(), likedAt), trackItem);
    }
}
