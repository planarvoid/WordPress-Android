package com.soundcloud.android.ads;

import static com.soundcloud.android.playback.VideoSurfaceProvider.Origin.PRESTITIAL;
import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
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

public class PrestitialPresenterTest extends AndroidUnitTest {

    @Mock Navigator navigator;
    @Mock AdViewabilityController viewabilityController;
    @Mock VisualPrestitialView visualPrestitialView;
    @Mock PrestitialAdsController adsController;
    @Mock PrestitialAdapterFactory adapterFactory;
    @Mock PrestitialAdapter adapter;
    @Mock SponsoredSessionVideoView sponsoredSessionVideoView;

    @Mock VideoSurfaceProvider videoSurfaceProvider;
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
                                            adPlayer,
                                            navigator,
                                            eventBus);
        when(adsController.getCurrentAd()).thenReturn(Optional.of(visualPrestitialAd));
    }

    // TODO:ADS Should we/how can we test the pager adapter setup?
    @Test
    public void finishesActivityOnContinueClick() {
        presenter.onCreate(activity, null);

        presenter.closePrestitial();

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

        verify(navigator).openAdClickthrough(context(), visualPrestitialAd.clickthroughUrl());
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
        eventBus.publish(EventQueue.INLAY_AD, AdPlayStateTransition.create(sponsoredSessionAd.video(), stateTransition, false, new Date(1)));

        verify(sponsoredSessionVideoView).setPlayState(stateTransition);
    }

    @Test
    public void pagerIsAdvancedWhenVideoFinishes() {
        setupSponsoredSession();
        setSponsoredSessionPage(1);

        final PlaybackStateTransition stateTransition = TestPlayerTransitions.complete();
        eventBus.publish(EventQueue.INLAY_AD, AdPlayStateTransition.create(sponsoredSessionAd.video(), stateTransition, false, new Date(1)));

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

    private void setupSponsoredSession() {
        when(adapterFactory.create(sponsoredSessionAd, presenter, sponsoredSessionVideoView)).thenReturn(adapter);
        when(adapter.getCount()).thenReturn(3);
        when(adapter.getPage(0)).thenReturn(PrestitialAdapter.PrestitialPage.OPT_IN_CARD);
        when(adapter.getPage(1)).thenReturn(PrestitialAdapter.PrestitialPage.VIDEO_CARD);
        when(adapter.getPage(2)).thenReturn(PrestitialAdapter.PrestitialPage.END_CARD);
        when(adsController.getCurrentAd()).thenReturn(Optional.of(sponsoredSessionAd));
    }

    private void setSponsoredSessionPage(int position) {
        presenter.onCreate(activity, null);
        ViewPager pager = (ViewPager) activity.findViewById(R.id.prestitial_pager);
        pager.setCurrentItem(position);
    }
}
