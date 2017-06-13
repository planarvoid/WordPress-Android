package com.soundcloud.android.playback.ui;

import static android.support.v4.view.PagerAdapter.POSITION_NONE;
import static com.soundcloud.android.playback.VideoSurfaceProvider.Origin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.InterstitialAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayOperations;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.VideoAdQueueItem;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.TestEventBus;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayerPagerPresenterTest extends AndroidUnitTest {

    private static final Urn TRACK1_URN = Urn.forTrack(123L);
    private static final Urn TRACK2_URN = Urn.forTrack(234L);
    private static final Urn TRACK2_RELATED_URN = Urn.forTrack(678L);
    private static final Urn AD_URN = Urn.forTrack(235L);
    private static final Urn MONETIZABLE_TRACK_URN = Urn.forTrack(456L);

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private TrackItemRepository trackRepository;
    @Mock private TrackPagePresenter trackPagePresenter;
    @Mock private IntroductoryOverlayOperations introductoryOverlayOperations;
    @Mock private AudioAdPresenter audioAdPresenter;
    @Mock private VideoAdPresenter videoAdPresenter;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private AdsOperations adOperations;
    @Mock private StationsOperations stationsOperations;
    @Mock private VideoSurfaceProvider videoSurfaceProvider;
    @Mock private PlayerPagerOnboardingPresenter onboardingPresenter;

    @Mock private PlayerTrackPager playerTrackPager;

    @Captor private ArgumentCaptor<SkipListener> skipListenerArgumentCaptor;
    @Captor private ArgumentCaptor<ViewPager.SimpleOnPageChangeListener> pageChangeListenerArgumentCaptor;
    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    @Mock private ViewGroup container;
    @Mock private ViewVisibilityProvider viewVisibilityProvider;

    @Mock private View view1;
    @Mock private View view2;
    @Mock private View view3;
    @Mock private View view4;
    @Mock private View view5;
    @Mock private View view6;
    @Mock private View audioAdView;
    @Mock private View videoAdView;

    @Mock private TextureView videoTextureView;

    @Mock private View fragmentView;
    @Mock private PlayerFragment playerFragment;
    @Mock private PlayerActivity playerActivity;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    private TestEventBus eventBus;
    private PlayerPagerPresenter presenter;
    private TrackItem track;
    private PagerAdapter adapter;

    private List<PlayQueueItem> playQueue = Arrays.asList(
            TestPlayQueueItem.createTrack(TRACK1_URN),
            TestPlayQueueItem.createTrack(TRACK2_URN),
            TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(MONETIZABLE_TRACK_URN)),
            TestPlayQueueItem.createTrack(AD_URN, AdFixtures.getInterstitialAd(MONETIZABLE_TRACK_URN)));

    @Before
    public void setUp() throws Exception {

        when(trackPagePresenter.createItemView(any(ViewGroup.class), any(SkipListener.class))).thenReturn(view1,
                                                                                                          view2,
                                                                                                          view3,
                                                                                                          view4,
                                                                                                          view5,
                                                                                                          view6);
        when(audioAdPresenter.createItemView(any(ViewGroup.class), any(SkipListener.class))).thenReturn(audioAdView);
        when(videoAdPresenter.createItemView(any(ViewGroup.class), any(SkipListener.class))).thenReturn(videoAdView);
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);

        eventBus = new TestEventBus();
        presenter = new PlayerPagerPresenter(playQueueManager,
                                             playSessionStateProvider,
                                             trackRepository,
                                             stationsOperations,
                                             trackPagePresenter,
                                             introductoryOverlayOperations,
                                             audioAdPresenter,
                                             videoAdPresenter,
                                             castConnectionHelper,
                                             adOperations,
                                             videoSurfaceProvider,
                                             onboardingPresenter,
                                             eventBus,
                                             performanceMetricsEngine);
        when(playerFragment.getPlayerPager()).thenReturn(playerTrackPager);
        when(container.getResources()).thenReturn(resources());
        presenter.onViewCreated(playerFragment, container, null);
        presenter.setCurrentPlayQueue(playQueue, 0);

        final ArgumentCaptor<PagerAdapter> pagerAdapterCaptor = ArgumentCaptor.forClass(PagerAdapter.class);
        verify(playerTrackPager).setAdapter(pagerAdapterCaptor.capture());
        adapter = pagerAdapterCaptor.getValue();

        presenter.setCurrentPlayQueue(playQueue, 0);


        track = ModelFixtures.trackItem(ModelFixtures.trackBuilder().urn(TRACK1_URN).title("title").creatorName("artist").creatorUrn(Urn.forUser(123L)).build());

        when(trackRepository.track(MONETIZABLE_TRACK_URN)).thenReturn(Maybe.just(
                ModelFixtures.trackItem(ModelFixtures.trackBuilder().urn(MONETIZABLE_TRACK_URN).title("title").creatorName("artist").creatorUrn(Urn.forUser(123L)).build())));

        when(trackRepository.track(TRACK2_RELATED_URN)).thenReturn(Maybe.just(
                ModelFixtures.trackItem(ModelFixtures.trackBuilder().urn(TRACK2_RELATED_URN).title("related title").creatorName("related artist").creatorUrn(Urn.forUser(234L)).build())));
    }

    @Test
    public void onNextOnSkipListenerSetsPagerToNextPosition() {
        verify(trackPagePresenter, atLeastOnce()).createItemView(same(playerTrackPager),
                                                                 skipListenerArgumentCaptor.capture());
        when(playerTrackPager.getCurrentItem()).thenReturn(3);

        skipListenerArgumentCaptor.getValue().onNext();

        verify(playerTrackPager).setCurrentItem(eq(4));
    }

    @Test
    public void onPreviousOnSkipListenerSetsPagerToPreviousPosition() {
        verify(trackPagePresenter, atLeastOnce()).createItemView(same(playerTrackPager),
                                                                 skipListenerArgumentCaptor.capture());
        when(playerTrackPager.getCurrentItem()).thenReturn(3);

        skipListenerArgumentCaptor.getValue().onPrevious();

        verify(playerTrackPager).setCurrentItem(eq(2));
    }

    @Test
    public void onPreviousOnSkipListenerSendsTrackingEvent() {
        verify(trackPagePresenter, atLeastOnce()).createItemView(same(playerTrackPager), skipListenerArgumentCaptor.capture());

        skipListenerArgumentCaptor.getValue().onPrevious();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.fromButtonSkip().getKind());
    }

    @Test
    public void onNextOnSkipListenerSendsTrackingEvent() {
        verify(trackPagePresenter, atLeastOnce()).createItemView(same(playerTrackPager), skipListenerArgumentCaptor.capture());

        skipListenerArgumentCaptor.getValue().onNext();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.fromButtonSkip().getKind());
    }

    @Test
    public void onSwipeSendsTrackingEvent() {
        presenter.onSwipe();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.fromSwipeSkip().getKind());
    }

    @Test
    public void onSwipeDuringPlaybackStartsMeasuring() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        presenter.onSwipe();

        verify(performanceMetricsEngine).startMeasuring(performanceMetricArgumentCaptor.capture());
        PerformanceMetric performanceMetric = performanceMetricArgumentCaptor.getValue();
        assertThat(performanceMetric.metricType()).isEqualTo(MetricType.TIME_TO_SKIP);
        assertThat(performanceMetric.metricParams().toBundle().getString(MetricKey.SKIP_ORIGIN.toString())).isEqualTo("swipe");
    }

    @Test
    public void onSwipeWhenNotPlayingDoesNotStartMeasuring() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        presenter.onSwipe();

        verify(performanceMetricsEngine, never()).startMeasuring(any(PerformanceMetric.class));
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateOnPresenter() {
        presenter.onResume(playerFragment);
        final View currentTrackView = getPageView();
        final PlayStateEvent stateEvent = TestPlayStates.playing(TRACK1_URN);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stateEvent);

        verify(trackPagePresenter).setPlayState(currentTrackView, stateEvent, true, true);
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateForOtherPage() {
        presenter.onResume(playerFragment);
        setCurrentTrackState(0, true);
        setCurrentTrackState(1, false);

        final View viewForCurrentTrack = getPageView(0);
        final View viewForOtherTrack = getPageView(1);

        Mockito.reset(trackPagePresenter);
        final PlayStateEvent stateEvent = TestPlayStates.playing(TRACK1_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stateEvent);

        verify(trackPagePresenter).setPlayState(viewForCurrentTrack, stateEvent, true, true);
        verify(trackPagePresenter).setPlayState(viewForOtherTrack, stateEvent, false, true);
    }

    @Test
    public void onPlayingStateEventForVideoCallsSetPlayStateForOnPresenter() {
        presenter.onResume(playerFragment);
        setupVideoAd();
        final VideoAd videoAd = AdFixtures.getVideoAd(MONETIZABLE_TRACK_URN);
        final View currentVideoView = getVideoAdPageView();

        PlayStateEvent stateEvent = TestPlayStates.playing(videoAd.adUrn(), 0, 100);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stateEvent);

        verify(videoAdPresenter).setPlayState(currentVideoView, stateEvent, true, true);
    }

    @Test
    public void onPlayableChangedEventSetsLikeStatusOnTrackPage() {
        View currentPageView = getPageView();
        LikesStatusEvent likeEvent = LikesStatusEvent.create(TRACK1_URN, true, 1);

        eventBus.publish(EventQueue.LIKE_CHANGED, likeEvent);

        verify(trackPagePresenter).onPlayableLiked(currentPageView, likeEvent.likes().values().iterator().next());
    }

    @Test
    public void onPlayableChangedEventIsIgnoredForPlaylistAssociations() {
        getPageView();
        LikesStatusEvent likeEvent = LikesStatusEvent.create(Urn.forPlaylist(123L), true, 1);

        eventBus.publish(EventQueue.LIKE_CHANGED, likeEvent);

        verify(trackPagePresenter, never()).onPlayableLiked(any(View.class), any(LikesStatusEvent.LikeStatus.class));
    }

    @Test
    public void onPlaybackProgressEventSetsProgressOnCurrentPlayingVideoPage() {
        final VideoAd videoAd = AdFixtures.getVideoAd(MONETIZABLE_TRACK_URN);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));

        setupVideoAd();
        presenter.onResume(playerFragment);
        View currentPageView = getVideoAdPageView();

        PlaybackProgressEvent event = PlaybackProgressEvent.create(new PlaybackProgress(5L, 10L, videoAd.adUrn()), videoAd.adUrn());

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(videoAdPresenter).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventDoesntSetProgressOnNonCurrentPlayingVideoPage() {
        final VideoAd videoAd = AdFixtures.getVideoAd(MONETIZABLE_TRACK_URN);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createVideo(videoAd));

        setupVideoAd();
        presenter.onResume(playerFragment);
        View currentPageView = getVideoAdPageView();

        final Urn urn = Urn.forAd("dfp", "other-ad-urn");
        PlaybackProgressEvent event = PlaybackProgressEvent.create(new PlaybackProgress(5L, 10L, urn),
                                                                   urn);

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(videoAdPresenter, never()).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventSetsProgressOnCurrentPlayingTrackPage() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK1_URN));
        presenter.onResume(playerFragment);
        View currentPageView = getPageView();
        PlaybackProgressEvent event = PlaybackProgressEvent.create(new PlaybackProgress(5L, 10L, TRACK1_URN), TRACK1_URN);

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventDoNotSetsProgressForPausedAdapter() {
        presenter.onResume(playerFragment);
        View currentPageView = getPageView();
        PlaybackProgressEvent event = PlaybackProgressEvent.create(new PlaybackProgress(5L, 10L, TRACK1_URN), TRACK1_URN);
        presenter.onPause(playerFragment);

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter, never()).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventDoNotSetProgressOnOtherTrackPage() {
        View currentPageView = getPageView();
        final Urn itemUrn = Urn.forTrack(234L);
        PlaybackProgressEvent event = PlaybackProgressEvent.create(new PlaybackProgress(5L, 10L, itemUrn),
                                                                   itemUrn);

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter, never()).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventDoesNotSetProgressOnNotPlayingTrackPage() {
        View currentPageView = getPageView();
        final Urn urn = Urn.forTrack(999L);
        PlaybackProgressEvent event = PlaybackProgressEvent.create(new PlaybackProgress(5L, 10L, urn),
                                                                   urn);

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter, never()).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void setsCollapsedModeOnSubscribeForCollapsePlayerEvent() {
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(trackPagePresenter, times(2)).setCollapsed(currentTrackView);
    }

    @Test
    public void setsCollapsedModeOnSubscribeForCollapsingPlayerEvent() {
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(trackPagePresenter, times(2)).setCollapsed(currentTrackView);
    }

    @Test
    public void setsExpandedModeOnSubscribeForExpandPlayerEvent() {
        verify(playerTrackPager).addOnPageChangeListener(pageChangeListenerArgumentCaptor.capture());
        pageChangeListenerArgumentCaptor.getValue().onPageSelected(0);
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(trackPagePresenter).setExpanded(currentTrackView, playQueue.get(0), true);
    }

    @Test
    public void creatingNewTrackViewSetThePlayState() {
        presenter.onResume(playerFragment);
        PlayStateEvent playStateEvent = TestPlayStates.playing(TRACK1_URN);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, playStateEvent);

        View currentPageView = getPageView();

        verify(trackPagePresenter).setPlayState(eq(currentPageView),
                                                any(PlayStateEvent.class),
                                                eq(true),
                                                eq(true));
    }

    @Test
    public void recyclingTrackViewDoesNotSetThePlayState() {
        final View view = getPageView(3);
        Mockito.reset(trackPagePresenter);
        when(trackPagePresenter.clearItemView(view)).thenReturn(view);
        when(trackPagePresenter.accept(view)).thenReturn(true);

        adapter.destroyItem(container, 3, view);

        View currentPageView = (View) adapter.instantiateItem(container, 3);

        verify(trackPagePresenter, never()).setPlayState(eq(currentPageView),
                                                         any(PlayStateEvent.class),
                                                         eq(true),
                                                         eq(true));
    }

    @Test
    public void getViewClearsRecycledViewWithUrnForCurrentPosition() {
        getPageView();
        verify(trackPagePresenter).clearItemView(any(View.class));
    }

    @Test
    public void getViewUsesCachedObservableIfAlreadyInCache() {
        getPageView();
        verify(trackRepository).track(TRACK1_URN);
    }

    @Test
    public void shouldCreateTrackViewForTracks() {
        assertThat(getPageView()).isSameAs(view6);
    }

    @Test
    public void shouldBindTrackViewForTracks() {
        ArgumentCaptor<PlayerTrackState> captorPlayerTrackState = ArgumentCaptor.forClass(PlayerTrackState.class);

        presenter.onResume(playerFragment);
        final View pageView = getPageView();

        verify(trackPagePresenter).bindItemView(same(pageView), captorPlayerTrackState.capture());

        assertThat(captorPlayerTrackState.getValue().getTrackUrn()).isEqualTo(TRACK1_URN);
        assertThat(captorPlayerTrackState.getValue().getTitle()).isEqualTo("title");
        assertThat(captorPlayerTrackState.getValue().getUserName()).isEqualTo("artist");
        assertThat(captorPlayerTrackState.getValue().isForeground()).isTrue();
        assertThat(captorPlayerTrackState.getValue().isCurrentTrack()).isTrue();
    }

    @Test
    public void shouldBindTrackViewForTrackWithRelatedTrack() {
        ArgumentCaptor<PlayerTrackState> captorPlayerTrackState = ArgumentCaptor.forClass(PlayerTrackState.class);

        presenter.onResume(playerFragment);

        final View pageView = getPageView();

        verify(trackPagePresenter).bindItemView(same(pageView), captorPlayerTrackState.capture());

        assertThat(captorPlayerTrackState.getValue().getTrackUrn()).isEqualTo(TRACK1_URN);
        assertThat(captorPlayerTrackState.getValue().getTitle()).isEqualTo("title");
        assertThat(captorPlayerTrackState.getValue().getUserName()).isEqualTo("artist");
        assertThat(captorPlayerTrackState.getValue().isForeground()).isTrue();
        assertThat(captorPlayerTrackState.getValue().isCurrentTrack()).isTrue();
    }

    @Test
    public void shouldCreateAdViewForAudioAds() {
        presenter.onViewCreated(playerFragment, container, null);
        getAudioAdPageView();

        verify(audioAdPresenter).createItemView(any(ViewGroup.class), any(SkipListener.class));
    }

    @Test
    public void shouldCreateVideoAdViewForVideoAds() {
        presenter.onViewCreated(playerFragment, container, null);
        setupVideoAd();
        getVideoAdPageView();

        verify(videoAdPresenter).createItemView(any(ViewGroup.class), any(SkipListener.class));
    }

    @Test
    public void shouldBindAdViewForAudioAds() {
        presenter.onResume(playerFragment);
        View pageView = getAudioAdPageView();
        ArgumentCaptor<AudioPlayerAd> captorAudioPlayerAd = ArgumentCaptor.forClass(AudioPlayerAd.class);

        verify(audioAdPresenter).bindItemView(eq(pageView), captorAudioPlayerAd.capture());

        assertThat(captorAudioPlayerAd.getValue().getImage()).isNotNull();
        assertThat(captorAudioPlayerAd.getValue().getMonetizableTrack()).isEqualTo(MONETIZABLE_TRACK_URN);
        assertThat(captorAudioPlayerAd.getValue().getPreviewTitle(resources())).isEqualTo("Next up: title (artist)");
    }

    @Test
    public void shouldBindAdViewForAudioAdsWhenTrackRepositoryReturnsEmpty() {
        when(trackRepository.track(MONETIZABLE_TRACK_URN)).thenReturn(Maybe.empty());
        presenter.onResume(playerFragment);
        View pageView = getAudioAdPageView();
        ArgumentCaptor<AudioPlayerAd> captorAudioPlayerAd = ArgumentCaptor.forClass(AudioPlayerAd.class);

        verify(audioAdPresenter).bindItemView(eq(pageView), captorAudioPlayerAd.capture());

        assertThat(captorAudioPlayerAd.getValue().getImage()).isNotNull();
        assertThat(captorAudioPlayerAd.getValue().getMonetizableTrack()).isEqualTo(MONETIZABLE_TRACK_URN);
        assertThat(captorAudioPlayerAd.getValue().getPreviewTitle(resources())).isEqualTo(Strings.EMPTY);
    }

    @Test
    public void shouldBindAdViewAndSetVideoSurfaceForVideoAds() {
        when(videoAdPresenter.getVideoTexture(any(View.class))).thenReturn(videoTextureView);
        when(videoAdPresenter.getViewabilityLayer(any(View.class))).thenReturn(view1);
        presenter.onResume(playerFragment);
        VideoAd ad = setupVideoAd();
        View pageView = getVideoAdPageView();
        ArgumentCaptor<VideoPlayerAd> captorVideoPlayerAd = ArgumentCaptor.forClass(VideoPlayerAd.class);

        verify(videoAdPresenter).bindItemView(eq(pageView), captorVideoPlayerAd.capture());
        verify(videoSurfaceProvider).setTextureView(ad.uuid(), Origin.PLAYER, videoTextureView, view1);

        assertThat(captorVideoPlayerAd.getValue().getMonetizableTrack()).isEqualTo(MONETIZABLE_TRACK_URN);
        assertThat(captorVideoPlayerAd.getValue().getPreviewTitle(resources())).isEqualTo("Next up: title (artist)");
    }

    @Test
    public void shouldBindAdViewAndSetVideoSurfaceForVideoAdsWhenTrackRepositoryReturnsEmpty() {
        when(trackRepository.track(MONETIZABLE_TRACK_URN)).thenReturn(Maybe.empty());
        when(videoAdPresenter.getVideoTexture(any(View.class))).thenReturn(videoTextureView);
        when(videoAdPresenter.getViewabilityLayer(any(View.class))).thenReturn(view1);
        presenter.onResume(playerFragment);
        VideoAd ad = setupVideoAd();
        View pageView = getVideoAdPageView();
        ArgumentCaptor<VideoPlayerAd> captorVideoPlayerAd = ArgumentCaptor.forClass(VideoPlayerAd.class);

        verify(videoAdPresenter).bindItemView(eq(pageView), captorVideoPlayerAd.capture());
        verify(videoSurfaceProvider).setTextureView(ad.uuid(), Origin.PLAYER, videoTextureView, view1);

        assertThat(captorVideoPlayerAd.getValue().getMonetizableTrack()).isEqualTo(MONETIZABLE_TRACK_URN);
        assertThat(captorVideoPlayerAd.getValue().getPreviewTitle(resources())).isEqualTo(Strings.EMPTY);
    }

    @Test
    public void resetAllScrubStateWhenTrackChange() {
        setCurrentTrackState(0, true);
        View currentPageView = getPageView(0);

        when(trackPagePresenter.accept(currentPageView)).thenReturn(true);

        presenter.onTrackChange();

        verify(trackPagePresenter).onPageChange(currentPageView);
    }

    @Test
    public void getItemPositionReturnsNoneForCustomRecyclingOfTrackViews() {
        assertThat(adapter.getItemPosition(getPageView())).isSameAs(POSITION_NONE);
    }

    @Test
    public void getItemPositionReturnsAdViewIndexIfCurrentAd() {
        when(adOperations.isCurrentItemAd()).thenReturn(true);

        presenter.onResume(playerFragment);

        assertThat(adapter.getItemPosition(getAudioAdPageView())).isEqualTo(2);
    }

    @Test
    public void getItemPositionReturnsNoneIfNotCurrentAd() {
        when(adOperations.isCurrentItemAd()).thenReturn(false);

        presenter.onResume(playerFragment);

        assertThat(adapter.getItemPosition(getAudioAdPageView())).isSameAs(POSITION_NONE);
    }

    @Test
    public void onPlayerSlideForwardsPositionToAdPresenter() {
        final View pageView = getAudioAdPageView();
        presenter.onPlayerSlide(0.5f);
        verify(audioAdPresenter).onPlayerSlide(pageView, 0.5f);
    }

    @Test
    public void onPlayerSlideForwardsPositionToTrackPresenter() {
        final View pageView = getPageView();
        presenter.onPlayerSlide(0.5f);
        verify(trackPagePresenter).onPlayerSlide(pageView, 0.5f);
    }

    @Test
    public void bindingTrackViewSetsPositionOnPresenter() {
        final View pageView = getPageView(1);

        verify(trackPagePresenter).onPositionSet(pageView, 1, 4);
    }

    @Test
    public void bindingTrackViewUpdatesCastDataOnPresenter() {
        final View pageView = getPageView(1);

        verify(trackPagePresenter).updateCastData(pageView, false);
    }

    @Test
    public void updatesCastDataOnCastAvaialable() {
        final View pageView = getPageView(1);

        presenter.onCastAvailable();

        verify(trackPagePresenter).updateCastData(pageView, true);
    }

    @Test
    public void onPauseSetsBackgroundStateOnPresenter() {
        final View pageView = getPageView();
        presenter.onPause(playerFragment);
        verify(trackPagePresenter).onBackground(pageView);
    }

    @Test
    public void onResumeSetsForegroundStateOnPresenter() {
        final View pageView = getPageView();
        presenter.onResume(playerFragment);
        verify(trackPagePresenter).onForeground(pageView);
    }

    @Test
    public void onResumeSetsVideoSurfacesForVideoItems() {
        when(videoAdPresenter.getVideoTexture(any(View.class))).thenReturn(videoTextureView);
        when(videoAdPresenter.getViewabilityLayer(any(View.class))).thenReturn(view1);
        VideoAd ad = setupVideoAd();
        getVideoAdPageView();

        presenter.onResume(playerFragment);

        verify(videoSurfaceProvider).setTextureView(ad.uuid(), Origin.PLAYER, videoTextureView, view1);
    }

    @Test
    public void configurationChangeForwardsToOnConfigurationChangeOnVideoSurfaceProvider() {
        onDestroy(true);

        verify(videoSurfaceProvider).onConfigurationChange(Origin.PLAYER);
    }

    @Test
    public void onDestroyForwardsCallToOnDestroyVideoSurfaceProvider() {
        onDestroy(false);

        verify(videoSurfaceProvider).onDestroy(Origin.PLAYER);
    }

    @Test
    public void reusingExistingViewSetsForegroundStateOnPresenter() {
        presenter.onResume(playerFragment);
        final View view = getPageView();
        when(trackPagePresenter.clearItemView(view)).thenReturn(view);
        when(trackPagePresenter.accept(view)).thenReturn(true);

        adapter.destroyItem(container, 0, view);

        View currentPageView = (View) adapter.instantiateItem(container, 0);

        verify(trackPagePresenter, times(2)).onForeground(currentPageView);
    }

    @Test
    public void destroyingViewDoesNotSetBackgroundStateOnPresenterOnCurrentTrack() {
        final View view = getPageView();
        when(trackPagePresenter.accept(view)).thenReturn(true);
        when(playQueueManager.isCurrentItem(playQueue.get(0))).thenReturn(true);

        adapter.destroyItem(container, 0, view);

        verify(trackPagePresenter, never()).onBackground(view);
    }

    @Test
    public void destroyingViewSetsBackgroundStateOnPresenter() {
        final View view = getPageView();
        when(trackPagePresenter.accept(view)).thenReturn(true);
        when(playQueueManager.isCurrentItem(playQueue.get(0))).thenReturn(false);

        adapter.destroyItem(container, 0, view);

        verify(trackPagePresenter).onBackground(view);
    }

    @Test
    public void instantiateItemForMonetizableSetsAdOverlay() throws Exception {
        final View viewForTrack = getPageView(3);
        final InterstitialAd interstitial = AdFixtures.getInterstitialAd(MONETIZABLE_TRACK_URN);
        verify(trackPagePresenter).setAdOverlay(same(viewForTrack), eq(interstitial));
    }

    @Test
    public void instantiateItemWithoutLeaveBehindClearsLeaveBehind() throws Exception {
        final View view = getPageView();
        verify(trackPagePresenter).clearAdOverlay(view);
    }

    @Test
    public void trackChangeEventDismissesLeaveBehindsOnNonPlayingTracks() throws Exception {
        final View viewForCurrentTrack = getPageView(1);
        final View viewForOtherTrack = getPageView(3);
        setCurrentTrackState(3, true);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(
                                 MONETIZABLE_TRACK_URN), Urn.NOT_SET, 0));

        verify(trackPagePresenter, times(2)).clearAdOverlay(viewForCurrentTrack);
        verify(trackPagePresenter, never()).clearAdOverlay(viewForOtherTrack);

    }

    @Test
    public void pageSelectedCallsPresenterWithSelectedPage() {
        final View view = getPageView();
        verify(playerTrackPager).addOnPageChangeListener(pageChangeListenerArgumentCaptor.capture());

        pageChangeListenerArgumentCaptor.getValue().onPageSelected(0);

        verify(trackPagePresenter, times(2)).onViewSelected(view, playQueue.get(0), false);
    }

    @Test
    public void pageSelectedCallsPresenterWithSelectedPageWhileExpanded() {
        final View view = getPageView();
        verify(playerTrackPager).addOnPageChangeListener(pageChangeListenerArgumentCaptor.capture());
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        pageChangeListenerArgumentCaptor.getValue().onPageSelected(0);

        verify(trackPagePresenter).onViewSelected(view, playQueue.get(0), true);
    }

    @Test
    public void expandedEventCallsPresenterWithSelectedState() {
        final View view = getPageView();
        verify(playerTrackPager).addOnPageChangeListener(pageChangeListenerArgumentCaptor.capture());
        pageChangeListenerArgumentCaptor.getValue().onPageSelected(0);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(trackPagePresenter).setExpanded(view, playQueue.get(0), true);
    }

    @Test
    public void gettingPageWithSelectedItemCallsOnViewSelected() {
        verify(playerTrackPager).addOnPageChangeListener(pageChangeListenerArgumentCaptor.capture());
        pageChangeListenerArgumentCaptor.getValue().onPageSelected(0);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        View currentPageView = getPageView();

        verify(trackPagePresenter).onViewSelected(currentPageView, playQueue.get(0), true);
    }

    @Test
    public void expansionOfPlayerWillForwardTheIntroductoryOverlayCallToTheTrackPage() {
        presenter.onResume(playerFragment);
        verify(playerTrackPager).addOnPageChangeListener(pageChangeListenerArgumentCaptor.capture());
        pageChangeListenerArgumentCaptor.getValue().onPageSelected(0);
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(trackPagePresenter).showIntroductoryOverlayForPlayQueue(currentTrackView);
    }

    @Test
    public void expansionOfPlayerWillOnlyForwardTheIntroductoryOverlayCallToTheSelectedTrackPage() {
        int selectedPagePosition = 1;
        int unselectedPagePosition = 0;

        View unselectedPage = getPageView(unselectedPagePosition);
        View selectedPage = getPageView(selectedPagePosition);
        getPageView(2);

        presenter.onResume(playerFragment);
        verify(playerTrackPager).addOnPageChangeListener(pageChangeListenerArgumentCaptor.capture());
        pageChangeListenerArgumentCaptor.getValue().onPageSelected(selectedPagePosition);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(trackPagePresenter, never()).showIntroductoryOverlayForPlayQueue(unselectedPage);
        verify(trackPagePresenter).showIntroductoryOverlayForPlayQueue(selectedPage);
    }

    @Test
    public void collapsingOfPlayerWillNotCallTheIntroductoryOverlayInTheTrackPage() {
        verify(playerTrackPager).addOnPageChangeListener(pageChangeListenerArgumentCaptor.capture());
        pageChangeListenerArgumentCaptor.getValue().onPageSelected(0);
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(trackPagePresenter, never()).showIntroductoryOverlayForPlayQueue(currentTrackView);
    }


    private View getVideoAdPageView() {
        return (View) adapter.instantiateItem(container, 0);
    }

    private View getPageView() {
        setCurrentTrackState(0, true);
        return getPageView(0);
    }

    private View getAudioAdPageView() {
        setCurrentTrackState(2, true);
        return getPageView(2);
    }

    private View getPageView(int position) {
        setupGetCurrentViewPreconditions(position);
        return (View) adapter.instantiateItem(container, position);
    }

    private void setupGetCurrentViewPreconditions(int position) {
        when(trackRepository.track(playQueue.get(position).getUrn())).thenReturn(Maybe.just(track));
    }

    private void setCurrentTrackState(int position, boolean isCurrentTrack) {
        when(playQueueManager.isCurrentItem(playQueue.get(position))).thenReturn(isCurrentTrack);
    }

    private VideoAd setupVideoAd() {
        final VideoAd video = AdFixtures.getVideoAd(MONETIZABLE_TRACK_URN);
        final VideoAdQueueItem videoItem = TestPlayQueueItem.createVideo(video);
        presenter.setCurrentPlayQueue(Collections.singletonList(videoItem), 0);
        return video;
    }

    private void onDestroy(boolean isConfigurationChange) {
        when(playerFragment.getView()).thenReturn(fragmentView);
        when(fragmentView.findViewById(anyInt())).thenReturn(playerTrackPager);
        when(playerFragment.getActivity()).thenReturn(playerActivity);
        when(playerActivity.isChangingConfigurations()).thenReturn(isConfigurationChange);
        presenter.onDestroyView(playerFragment);
    }
}
