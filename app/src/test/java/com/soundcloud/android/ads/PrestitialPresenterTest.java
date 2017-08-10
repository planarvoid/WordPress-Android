package com.soundcloud.android.ads;

import static com.soundcloud.android.events.AdPlaybackEvent.AdProgressEvent;
import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static com.soundcloud.android.playback.VideoSurfaceProvider.Origin.PRESTITIAL;
import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.PrestitialAdapter.PrestitialPage;
import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.SponsoredSessionStartEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackProgress;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import java.util.Date;
import java.util.List;

public class PrestitialPresenterTest extends AndroidUnitTest {

    @Mock Navigator navigator;
    @Mock AdViewabilityController viewabilityController;
    @Mock VisualPrestitialView visualPrestitialView;
    @Mock PrestitialAdsController adsController;
    @Mock PrestitialAdapterFactory adapterFactory;
    @Mock PrestitialAdapter adapter;
    @Mock SponsoredSessionVideoView sponsoredSessionVideoView;

    @Mock VideoSurfaceProvider videoSurfaceProvider;
    @Mock WhyAdsDialogPresenter whyAdsDialogPresenter;
    @Mock AdPlayer adPlayer;

    @Mock Intent intent;
    @Mock ImageView imageView;

    private AppCompatActivity activity;

    private TestEventBus eventBus;
    private VisualPrestitialAd visualPrestitialAd;
    private SponsoredSessionAd sponsoredSessionAd;
    private PrestitialPresenter presenter;

    @Before
    public void setUp() {
        activity = spy(activity());
        activity.setContentView(R.layout.sponsored_session_prestitial);
        visualPrestitialAd = AdFixtures.visualPrestitialAd();
        sponsoredSessionAd = AdFixtures.sponsoredSessionAd();
        eventBus = new TestEventBus();
        presenter = new PrestitialPresenter(adsController,
                                            viewabilityController,
                                            adapterFactory,
                                            lazyOf(visualPrestitialView),
                                            lazyOf(sponsoredSessionVideoView),
                                            videoSurfaceProvider,
                                            whyAdsDialogPresenter,
                                            adPlayer,
                                            navigator,
                                            eventBus);
        when(adsController.getCurrentAd()).thenReturn(Optional.of(visualPrestitialAd));
    }

    @Test
    public void finishesActivityOnContinueClick() {
        presenter.onCreate(activity, null);

        presenter.onContinueClick();

        verify(activity).finish();
    }

    @Test
    public void setsUpViewAndAdapterOnCreateWhenSponsoredSessionAdIsPresent() {
        when(adsController.getCurrentAd()).thenReturn(Optional.of(sponsoredSessionAd));
        when(adapterFactory.create(sponsoredSessionAd, presenter, sponsoredSessionVideoView)).thenReturn(adapter);

        verify(activity).setContentView(R.layout.sponsored_session_prestitial);
    }

    @Test
    public void finishesActivityWithAbsentAd() {
        when(adsController.getCurrentAd()).thenReturn(Optional.absent());

        presenter.onCreate(activity, null);

        verifyZeroInteractions(adapterFactory);
        verify(activity).finish();
    }

    @Test
    public void navigatesToClickThroughOnImageClickForVisualPrestitial() {
        presenter.onCreate(activity, null);

        presenter.onImageClick(activity, visualPrestitialAd, Optional.absent());

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forAdClickthrough(visualPrestitialAd.clickthroughUrl().toString()))));
    }

    @Test
    public void navigatesToClickThroughOnImageClickForSponsoredSessionAdEndCard() {
        presenter.onCreate(activity, null);

        presenter.onImageClick(activity, sponsoredSessionAd, Optional.of(PrestitialPage.END_CARD));

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forAdClickthrough(sponsoredSessionAd.clickthroughUrl().toString()))));
    }

    @Test
    public void navigatesToNextPageOnImageClickForSponsoredSessionOptInCard() {
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        presenter.onImageClick(context(), sponsoredSessionAd, Optional.of(PrestitialPage.OPT_IN_CARD));

        ViewPager pager = (ViewPager) activity.findViewById(R.id.prestitial_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(2);
    }

    @Test
    public void publishesUIEventOnClickThroughForVisualPrestitial() {
        presenter.onCreate(activity, null);

        presenter.onImageClick(activity, visualPrestitialAd, Optional.absent());

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UIEvent.class);
    }

    @Test
    public void publishesUIEventOnClickThroughForSponsoredSession() {
        presenter.onCreate(activity, null);

        presenter.onImageClick(activity, sponsoredSessionAd, Optional.of(PrestitialPage.END_CARD));

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UIEvent.class);
    }

    @Test
    public void finishesActivityOnClickThrough() {
        presenter.onCreate(this.activity, null);

        presenter.onImageClick(activity, visualPrestitialAd, Optional.absent());

        verify(this.activity).finish();
    }

    @Test
    public void publishesImpressionOnImageLoadCompleteForVisualPrestitial() {
        presenter.onImageLoadComplete(visualPrestitialAd, imageView, Optional.absent());

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PrestitialAdImpressionEvent.class);
    }

    @Test
    public void publishesImpressionOnImageLoadCompleteAndImageIsOnCurrentPageForSponsoredSession() {
        setupSponsoredSession();
        final PrestitialPage currentPage = PrestitialPage.END_CARD;
        setSponsoredSessionPage(currentPage.ordinal());
        presenter.onImageLoadComplete(sponsoredSessionAd, imageView, Optional.of(currentPage));

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PrestitialAdImpressionEvent.class);
    }

    @Test
    public void defersPublishOfImpressionIfImageLoadedIsNotOnCurrentPageUntilPageIsVisible() {
        setupSponsoredSession();
        final PrestitialPage currentPage = PrestitialPage.END_CARD;
        final int previousPageIndex = currentPage.ordinal() - 1;
        setSponsoredSessionPage(previousPageIndex);
        // Image loads but is not on screen
        presenter.onImageLoadComplete(sponsoredSessionAd, imageView, Optional.of(currentPage));

        // Pager navigates to page where that contains image
        setSponsoredSessionPage(currentPage.ordinal());

        final List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events.size()).isEqualTo(3);
        assertThat(events.get(0)).isInstanceOf(PrestitialAdImpressionEvent.class);
        assertThat(events.get(1)).isInstanceOf(PrestitialAdImpressionEvent.class);
        assertThat(events.get(2)).isInstanceOf(SponsoredSessionStartEvent.class);

        // Only fires impression once
        setSponsoredSessionPage(currentPage.ordinal());
        assertThat(events.size()).isEqualTo(3);
    }

    @Test
    public void publishesImpressionEventWhenOptInCardShowsUp() {
        setupSponsoredSession();
        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(1);
        assertThat(eventBus.firstEventOn(EventQueue.TRACKING)).isInstanceOf(PrestitialAdImpressionEvent.class);
    }

    @Test
    public void publishesSessionStartEventAndClearsAdsWhenPagerSwitchedToEndCard() {
        setupSponsoredSession();
        setSponsoredSessionPage(PrestitialPage.END_CARD.ordinal());

        verify(adsController).clearAllExistingAds();
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(SponsoredSessionStartEvent.class);
    }

    @Test
    public void startsViewabilityTrackingOnImageLoadCompleteForVisualPrestitial() {
        presenter.onImageLoadComplete(visualPrestitialAd, imageView, Optional.absent());

        verify(viewabilityController).startDisplayTracking(imageView, visualPrestitialAd);
    }

    @Test
    public void adjustsVideoLayoutWhenVideoViewIsSelected() {
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        verify(sponsoredSessionVideoView).adjustLayoutForVideo(sponsoredSessionAd.video());
    }

    @Test
    public void adPlayerPlaysVideoWhenVideoViewIsSelected() {
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        verify(adPlayer).play(sponsoredSessionAd.video(), true);
    }

    @Test
    public void stateTransitionsAreForwardedToVideoView() {
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        final PlaybackStateTransition stateTransition = TestPlayerTransitions.playing();
        eventBus.publish(EventQueue.AD_PLAYBACK, AdPlayStateTransition.create(sponsoredSessionAd.video(), stateTransition, false, new Date(1)));

        verify(sponsoredSessionVideoView).setPlayState(stateTransition);
    }

    @Test
    public void progressEventsAreForwardedToVideoView() {
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        final PlaybackProgress progress = TestPlaybackProgress.getPlaybackProgress(10, 100);
        eventBus.publish(EventQueue.AD_PLAYBACK, AdProgressEvent.create(sponsoredSessionAd.video(), progress, new Date(1)));

        verify(sponsoredSessionVideoView).setProgress(progress);
    }

    @Test
    public void pagerIsAdvancedWhenVideoFinishes() {
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        final PlaybackStateTransition stateTransition = TestPlayerTransitions.complete();
        eventBus.publish(EventQueue.AD_PLAYBACK, AdPlayStateTransition.create(sponsoredSessionAd.video(), stateTransition, false, new Date(1)));

        ViewPager pager = (ViewPager) activity.findViewById(R.id.prestitial_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(2);
    }

    @Test
    public void pagerIsAdvancedWhenVideoHasAnError() {
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        final PlaybackStateTransition stateTransition = TestPlayerTransitions.error(PlayStateReason.ERROR_FORBIDDEN);
        eventBus.publish(EventQueue.AD_PLAYBACK, AdPlayStateTransition.create(sponsoredSessionAd.video(), stateTransition, false, new Date(1)));

        ViewPager pager = (ViewPager) activity.findViewById(R.id.prestitial_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(2);
    }

    @Test
    public void onTextureViewBindSetsViewUpInVideoSurfaceProvider() {
        final View viewabilityLayer = mock(View.class);
        final TextureView textureView = mock(TextureView.class);

        presenter.onVideoTextureBind(textureView, viewabilityLayer, sponsoredSessionAd.video());

        verify(videoSurfaceProvider).setTextureView(sponsoredSessionAd.video().uuid(), PRESTITIAL, textureView, viewabilityLayer);
    }

    @Test
    public void stopsViewabilityTrackingOnDestroy() {
        presenter.onDestroy(activity);

        verify(viewabilityController).stopDisplayTracking();
    }

    @Test
    public void resetsAdPlayerOnDestroy() {
        presenter.onDestroy(activity);

        verify(adPlayer).reset();
    }

    @Test
    public void onDestroyForwardedToVideoSurfaceProvider() {
        presenter.onDestroy(activity);

        verify(videoSurfaceProvider).onDestroy(PRESTITIAL);
    }

    @Test
    public void onConfigurationChangeForwardedToVideoSurfaceProvider() {
        when(activity.isChangingConfigurations()).thenReturn(true);
        presenter.onDestroy(activity);

        verify(videoSurfaceProvider).onConfigurationChange(PRESTITIAL);
    }

    @Test
    public void onWhyAdsClickForwardsCallToWhyAdsPresenter() {
        presenter.onWhyAdsClicked(context());

        verify(whyAdsDialogPresenter).show(context());
    }

    @Test
    public void onSkipAdPausesVideoAndAdvancesPage() {
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        presenter.onSkipAd();

        verify(adPlayer).pause();
        ViewPager pager = (ViewPager) activity.findViewById(R.id.prestitial_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(2);
    }

    @Test
    public void togglePlaybackForwardsCallToAdPlayerIfAdPlayerIsHasBeenSetup() {
        when(adPlayer.getCurrentAd()).thenReturn(Optional.of(sponsoredSessionAd.video()));
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        presenter.onTogglePlayback();

        verify(adPlayer).togglePlayback(sponsoredSessionAd.video());
    }

    @Test
    public void togglePlaybackDoesNothingIfAdPlayerIsntSetup() {
        when(adPlayer.getCurrentAd()).thenReturn(Optional.absent());
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        presenter.onTogglePlayback();

        verify(adPlayer, never()).togglePlayback(sponsoredSessionAd.video());
    }

    @Test
    public void onPauseShouldPausePlaybackIfPlayerIsPlaying() {
        when(adPlayer.getCurrentAd()).thenReturn(Optional.of(sponsoredSessionAd.video()));
        when(adPlayer.isPlaying()).thenReturn(true);
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        presenter.onPause(activity);

        verify(adPlayer).pause();
    }

    @Test
    public void onPauseShouldDoNothingIfPlayerIsntPlaying() {
        when(adPlayer.getCurrentAd()).thenReturn(Optional.of(sponsoredSessionAd.video()));
        when(adPlayer.isPlaying()).thenReturn(false);
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        presenter.onPause(activity);

        verify(adPlayer, never()).pause();
    }

    @Test
    public void onResumeShouldReattachVideoSurfaceIfPlayerIsSetup() {
        final View viewabilityLayer = mock(View.class);
        final TextureView textureView = mock(TextureView.class);
        sponsoredSessionVideoView.viewabilityLayer = viewabilityLayer;
        sponsoredSessionVideoView.videoView = textureView;
        when(adPlayer.getCurrentAd()).thenReturn(Optional.of(sponsoredSessionAd.video()));
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        presenter.onResume(activity);

        verify(videoSurfaceProvider).setTextureView(sponsoredSessionAd.video().uuid(), PRESTITIAL, textureView, viewabilityLayer);
    }

    @Test
    public void onResumeShouldNotReattachVideoSurfaceIfPlayerHasOtherAdType() {
        final View viewabilityLayer = mock(View.class);
        final TextureView textureView = mock(TextureView.class);
        sponsoredSessionVideoView.viewabilityLayer = viewabilityLayer;
        sponsoredSessionVideoView.videoView = textureView;
        when(adPlayer.getCurrentAd()).thenReturn(Optional.of(AdFixtures.getVideoAd(Urn.forAd("123", "abc"))));
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        presenter.onResume(activity);

        verify(videoSurfaceProvider, never()).setTextureView(sponsoredSessionAd.video().uuid(), PRESTITIAL, textureView, viewabilityLayer);
    }

    @Test
    public void onOptionOneClickForOptInCardEndsActivity() {
        setupSponsoredSession();
        setSponsoredSessionPage(0);

        presenter.onOptionOneClick(PrestitialPage.OPT_IN_CARD, sponsoredSessionAd, context());

        verify(activity).finish();
    }

    @Test
    public void onOptionOneClickForEndCardOpensClickthrough() {
        setupSponsoredSession();
        setSponsoredSessionPage(2);

        presenter.onOptionOneClick(PrestitialPage.END_CARD, sponsoredSessionAd, activity);
        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forAdClickthrough(sponsoredSessionAd.optInCard().clickthroughUrl()))));
    }

    @Test
    public void onOptionTwoClickForOptInCardAdvancesPages() {
        setupSponsoredSession();
        setSponsoredSessionPage(0);

        presenter.onOptionTwoClick(PrestitialPage.OPT_IN_CARD, sponsoredSessionAd);

        ViewPager pager = (ViewPager) activity.findViewById(R.id.prestitial_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(1);
    }

    @Test
    public void onOptionTwoClickForEndCardEndsActivity() {
        setupSponsoredSession();
        setSponsoredSessionPage(2);

        presenter.onOptionTwoClick(PrestitialPage.END_CARD, sponsoredSessionAd);

        verify(activity).finish();
    }

    private void setupSponsoredSession() {
        when(adapterFactory.create(sponsoredSessionAd, presenter, sponsoredSessionVideoView)).thenReturn(adapter);
        when(adapter.getCount()).thenReturn(3);
        when(adapter.getPage(0)).thenReturn(PrestitialPage.OPT_IN_CARD);
        when(adapter.getPage(1)).thenReturn(PrestitialPage.VIDEO_CARD);
        when(adapter.getPage(2)).thenReturn(PrestitialPage.END_CARD);
        when(adsController.getCurrentAd()).thenReturn(Optional.of(sponsoredSessionAd));
        presenter.onCreate(activity, null);
    }

    private void setSponsoredSessionPage(int position) {
        ViewPager pager = (ViewPager) activity.findViewById(R.id.prestitial_pager);
        pager.setCurrentItem(position);
    }
}
