package com.soundcloud.android.likes;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.downloading;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static java.util.Collections.singletonList;
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
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
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

import android.view.View;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TrackLikesPresenterTest extends AndroidUnitTest {

    private static final int PAGE_SIZE = 2;
    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

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

    private final PublishSubject<TrackLikesPage> likedTracksObservable = PublishSubject.create();
    private final Observable<List<Urn>> likedTrackUrns = Observable.just(Arrays.asList(Urn.forTrack(1),
                                                                                       Urn.forTrack(2)));
    private TestSubscriber testSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setup() {
        when(adapterFactory.create(headerPresenter)).thenReturn(adapter);
        presenter = new TrackLikesPresenter(likeOperations,
                                            playbackInitiator,
                                            offlineContentOperations,
                                            adapterFactory,
                                            headerPresenter,
                                            expandPlayerSubscriberProvider,
                                            eventBus,
                                            swipeRefreshAttacher,
                                            dataSource);
        when(dataSource.initialTrackLikes()).thenReturn(likedTracksObservable);
        when(likeOperations.likedTrackUrns()).thenReturn(likedTrackUrns);
        when(likeOperations.onTrackLiked()).thenReturn(Observable.<PropertySet>empty());
        when(likeOperations.onTrackUnliked()).thenReturn(Observable.<Urn>empty());
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

        testSubscriber.assertReceivedOnNext(Arrays.asList(playbackResult));
    }

    @Test
    public void shouldNotSendUpsellEventOnMidTierItemClickWhenUserCannotUpgrade() {
        final TrackLikesTrackItem clickedTrack = new TrackLikesTrackItem(TrackItem.from(TestPropertySets.highTierTrack()));
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

        likedTracksObservable.onNext(TrackLikesPage.withHeader(Collections.<PropertySet>emptyList()));
        likedTracksObservable.onNext(TrackLikesPage.withHeader(Collections.<PropertySet>emptyList()));

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
        final List<PropertySet> tracks = Arrays.asList(TestPropertySets.fromApiTrack());
        when(likeOperations.likedTracks()).thenReturn(Observable.just(tracks));

        dataSource = new DataSource(likeOperations, PAGE_SIZE);
        dataSource.initialTrackLikes().subscribe(testSubscriber);

        testSubscriber.assertReceivedOnNext(Arrays.asList(TrackLikesPage.withHeader(tracks)));
    }

    @Test
    public void dataSourceUpdatedTrackLikesReturnsLikedTracksWithHeader() {
        final List<PropertySet> tracks = singletonList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(likeOperations.updatedLikedTracks()).thenReturn(Observable.just(tracks));

        new DataSource(likeOperations, PAGE_SIZE).updatedTrackLikes().subscribe(testSubscriber);

        testSubscriber.assertReceivedOnNext(Arrays.asList(TrackLikesPage.withHeader(tracks)));
    }

    @Test
    public void dataSourcePagerReturnsLikedTracksWithHeader() {
        final Date oldestDate = new Date(1);
        final PropertySet likedTrack1 = TestPropertySets.expectedLikedTrackForLikesScreenWithDate(oldestDate);
        final PropertySet likedTrack2 = TestPropertySets.expectedLikedTrackForLikesScreenWithDate(new Date(2));
        final List<PropertySet> tracks = Arrays.asList(likedTrack2, likedTrack1);
        when(likeOperations.likedTracks(oldestDate.getTime())).thenReturn(Observable.just(tracks));

        new DataSource(likeOperations, PAGE_SIZE).pagingFunction().call(TrackLikesPage.withoutHeader(tracks)).subscribe(testSubscriber);

        testSubscriber.assertReceivedOnNext(Arrays.asList(TrackLikesPage.withoutHeader(tracks)));
    }

    @Test
    public void trackPagerFinishesIfLastPageIncomplete() throws Exception {
        final List<PropertySet> tracks = singletonList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(likeOperations.likedTracks()).thenReturn(Observable.just(tracks));

        final Pager.PagingFunction<TrackLikesPage> listPager = new DataSource(likeOperations, PAGE_SIZE).pagingFunction();

        assertThat(listPager.call(TrackLikesPage.withHeader(tracks))).isSameAs(Pager.<List<PropertySet>>finish());
    }
}
