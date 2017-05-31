package com.soundcloud.android.ads;

import static com.soundcloud.android.events.AdPlaybackEvent.AdProgressEvent;
import static com.soundcloud.android.playback.VideoSurfaceProvider.Origin.PRESTITIAL;
import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.PrestitialAdapter.PrestitialPage;
import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.UIEvent;
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
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import java.util.Date;

public class PrestitialPresenterTest extends AndroidUnitTest {

    @Mock NavigationExecutor navigationExecutor;
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
                                            navigationExecutor,
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
    public void navigatesToClickThroughUrlOnClickThrough() {
        final View imageView = new View(context());
        presenter.onCreate(activity, null);

        presenter.onClickThrough(imageView, visualPrestitialAd);

        verify(navigationExecutor).openAdClickthrough(context(), visualPrestitialAd.clickthroughUrl());
    }

    @Test
    public void publishesUIEventOnClickThrough() {
        final View imageView = new View(context());
        presenter.onCreate(activity, null);

        presenter.onClickThrough(imageView, visualPrestitialAd);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UIEvent.class);
    }

    @Test
    public void finishesActivityOnClickThrough() {
        final View imageView = new View(context());
        presenter.onCreate(activity, null);

        presenter.onClickThrough(imageView, visualPrestitialAd);

        verify(activity).finish();
    }

    @Test
    public void publishesImpressionOnImageLoadComplete() {
        presenter.onImageLoadComplete(visualPrestitialAd, imageView);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PrestitialAdImpressionEvent.class);
    }

    @Test
    public void startsViewabilityTrackingOnImageLoadComplete(){
        presenter.onImageLoadComplete(visualPrestitialAd, imageView);

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
    public void onTextureViewBindSetsViewUpInVideoSurfaceProvider() {
        final View viewabilityLayer = mock(View.class);
        final TextureView textureView = mock(TextureView.class);

        presenter.onVideoTextureBind(textureView, viewabilityLayer, sponsoredSessionAd.video());

        verify(videoSurfaceProvider).setTextureView(sponsoredSessionAd.video().uuid(), PRESTITIAL, textureView, viewabilityLayer);
    }

    @Test
    public void stopsViewabilityTrackingOnDestroy(){
        presenter.onDestroy(activity);

        verify(viewabilityController).stopDisplayTracking();
    }

    @Test
    public void resetsAdPlayerOnDestroy(){
        presenter.onDestroy(activity);

        verify(adPlayer).reset();
    }

    @Test
    public void onDestroyForwardedToVideoSurfaceProvider(){
        presenter.onDestroy(activity);

        verify(videoSurfaceProvider).onDestroy(PRESTITIAL);
    }

    @Test
    public void onConfigurationChangeForwardedToVideoSurfaceProvider(){
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

        presenter.onOptionOneClick(PrestitialPage.END_CARD, sponsoredSessionAd, context());

        verify(navigationExecutor).openAdClickthrough(context(), Uri.parse(sponsoredSessionAd.optInCard().clickthroughUrl()));
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
    }

    private void setSponsoredSessionPage(int position) {
        presenter.onCreate(activity, null);
        ViewPager pager = (ViewPager) activity.findViewById(R.id.prestitial_pager);
        pager.setCurrentItem(position);
    }
}
