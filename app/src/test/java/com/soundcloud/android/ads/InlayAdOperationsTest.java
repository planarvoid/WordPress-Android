package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;

public class InlayAdOperationsTest extends AndroidUnitTest {
    private static AppInstallAd appInstall() {
        return AppInstallAd.create(AdFixtures.getApiAppInstall(), 424242);
    }

    private InlayAdOperations operations;
    private InlayAdOperations.OnScreenAndImageLoaded filter;

    @Mock InlayAdHelper inlayAdHelper;
    @Mock InlayAdPlayer inlayAdPlayer;

    TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        operations = new InlayAdOperations(eventBus, InjectionSupport.lazyOf(inlayAdPlayer));
        filter = new InlayAdOperations.OnScreenAndImageLoaded(inlayAdHelper);
    }

    @Test
    public void trackImpressionsIncludesOnScreenEventsWithImageLoadedBeforeEvent() {
        final Date beforeLoad = new Date(99);
        final Date loadTime = new Date(100);
        final Date afterLoad = new Date(101);

        final AppInstallAd unloaded = appInstall();
        final AppInstallAd loaded = appInstall();
        loaded.setImageLoadTimeOnce(loadTime);

        assertThat(filter.call(InlayAdEvent.OnScreen.create(42, loaded, beforeLoad))).isFalse();
        assertThat(filter.call(InlayAdEvent.OnScreen.create(42, unloaded, afterLoad))).isFalse();
        assertThat(filter.call(InlayAdEvent.OnScreen.create(42, loaded, afterLoad))).isTrue();
    }

    @Test
    public void trackImpressionsIncludesImageLoadedEventsForAdsOnScreen() {
        final int onScreen = 42;
        final int offScreen = 43;
        final AppInstallAd ad = appInstall();

        when(inlayAdHelper.isOnScreen(onScreen)).thenReturn(true);
        when(inlayAdHelper.isOnScreen(offScreen)).thenReturn(false);

        assertThat(filter.call(InlayAdEvent.ImageLoaded.create(onScreen, ad, new Date(1)))).isTrue();
        assertThat(filter.call(InlayAdEvent.ImageLoaded.create(offScreen, ad, new Date(2)))).isFalse();
    }

    @Test
    public void toImpressionSetsImpressionReported() {
        when(inlayAdHelper.isOnScreen(42)).thenReturn(true);
        final AppInstallAd ad = appInstall();

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.ImageLoaded.create(42, ad, new Date(999)));

        InlayAdImpressionEvent impressionEvent = (InlayAdImpressionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(impressionEvent.ad()).isEqualTo(ad.getAdUrn());
        assertThat(impressionEvent.contextPosition()).isEqualTo(42);
        assertThat(impressionEvent.getTimestamp()).isEqualTo(999);
    }

    @Test
    public void playsInlayAdPlayerWhenPlayerForVideoOnScreen() {
        when(inlayAdPlayer.isPlaying()).thenReturn(false);
        final VideoAd videoAd = AdFixtures.getVideoAd(1L);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.OnScreen.create(12, videoAd, new Date(999)));

        verify(inlayAdPlayer).play(videoAd, false);
    }

    @Test
    public void playsInlayAdPlayerWhenPlayerForVideoOnScreenEvenIfPlayerAlreadyPlaying() {
        when(inlayAdPlayer.isPlaying()).thenReturn(true);
        final VideoAd videoAd = AdFixtures.getVideoAd(1L);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.OnScreen.create(12, videoAd, new Date(999)));

        verify(inlayAdPlayer).play(videoAd, false);
    }

    @Test
    public void pausesInlayAdPlayerWhenPlayerIsPlayingForNoVideoOnScreen() {
        when(inlayAdPlayer.isPlaying()).thenReturn(true);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.NoVideoOnScreen.create(new Date(999)));

        verify(inlayAdPlayer).muteAndPause();
    }

    @Test
    public void doesNotPauseInlayAdPlayerWhenPlayerNotPlayingForNoVideoOnScreen() {
        when(inlayAdPlayer.isPlaying()).thenReturn(false);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.NoVideoOnScreen.create(new Date(999)));

        verify(inlayAdPlayer, never()).muteAndPause();;
    }

    @Test
    public void toggleMuteCommandForwardsCallToInlayAdPlayer() {
        final VideoAd videoAd = AdFixtures.getVideoAd(1L);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.ToggleVolume.create(0, videoAd, new Date(999)));

        verify(inlayAdPlayer).toggleVolume();
    }

    @Test
    public void doesNotDoAnythingWithStateTransitions() {
        final VideoAd videoAd = AdFixtures.getVideoAd(1L);
        final InlayAdEvent.InlayPlayStateTransition event = InlayAdEvent.InlayPlayStateTransition.create(videoAd, TestPlayerTransitions.idle(), false, new Date(999));

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.INLAY_AD, event);

        verifyZeroInteractions(inlayAdPlayer);
        verifyZeroInteractions(inlayAdHelper);
    }

    @Test
    public void forwardsPlaybackToggleToInlayPlayer() {
        final VideoAd videoAd = AdFixtures.getVideoAd(1L);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.TogglePlayback.create(0, videoAd, new Date(999)));

        verify(inlayAdPlayer).togglePlayback(videoAd);
    }
}
