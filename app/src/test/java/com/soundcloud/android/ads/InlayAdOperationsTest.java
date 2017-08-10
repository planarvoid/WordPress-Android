package com.soundcloud.android.ads;

import static com.soundcloud.android.events.AdPlaybackEvent.InlayAdEvent;
import static com.soundcloud.android.events.AdPlaybackEvent.NoVideoOnScreen;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.android.events.EventQueue;
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
    @Mock AdPlayer adPlayer;

    TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        operations = new InlayAdOperations(eventBus, InjectionSupport.lazyOf(adPlayer));
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

        assertThat(filter.call(InlayAdEvent.forOnScreen(42, loaded, beforeLoad))).isFalse();
        assertThat(filter.call(InlayAdEvent.forOnScreen(42, unloaded, afterLoad))).isFalse();
        assertThat(filter.call(InlayAdEvent.forOnScreen(42, loaded, afterLoad))).isTrue();
    }

    @Test
    public void trackImpressionsIncludesImageLoadedEventsForAdsOnScreen() {
        final int onScreen = 42;
        final int offScreen = 43;
        final AppInstallAd ad = appInstall();

        when(inlayAdHelper.isOnScreen(onScreen)).thenReturn(true);
        when(inlayAdHelper.isOnScreen(offScreen)).thenReturn(false);

        assertThat(filter.call(InlayAdEvent.forImageLoaded(onScreen, ad, new Date(1)))).isTrue();
        assertThat(filter.call(InlayAdEvent.forImageLoaded(offScreen, ad, new Date(2)))).isFalse();
    }

    @Test
    public void toImpressionSetsImpressionReported() {
        when(inlayAdHelper.isOnScreen(42)).thenReturn(true);
        final AppInstallAd ad = appInstall();

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.AD_PLAYBACK, InlayAdEvent.forImageLoaded(42, ad, new Date(999)));

        InlayAdImpressionEvent impressionEvent = (InlayAdImpressionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(impressionEvent.ad()).isEqualTo(ad.adUrn());
        assertThat(impressionEvent.contextPosition()).isEqualTo(42);
        assertThat(impressionEvent.getTimestamp()).isEqualTo(999);
    }
    
    @Test
    public void playsInlayAdPlayerWhenPlayerForVideoOnScreen() {
        when(adPlayer.isPlaying()).thenReturn(false);
        final VideoAd videoAd = AdFixtures.getInlayVideoAd(1L);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.AD_PLAYBACK, InlayAdEvent.forOnScreen(12, videoAd, new Date(999)));

        verify(adPlayer).autoplay(videoAd);
    }

    @Test
    public void playsInlayAdPlayerWhenPlayerForVideoOnScreenEvenIfPlayerAlreadyPlaying() {
        when(adPlayer.isPlaying()).thenReturn(true);
        final VideoAd videoAd = AdFixtures.getInlayVideoAd(1L);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.AD_PLAYBACK, InlayAdEvent.forOnScreen(12, videoAd, new Date(999)));

        verify(adPlayer).autoplay(videoAd);
    }

    @Test
    public void pausesInlayAdPlayerWhenPlayerIsPlayingForNoVideoOnScreenWithMuteTrue() {
        when(adPlayer.isPlaying()).thenReturn(true);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.AD_PLAYBACK, NoVideoOnScreen.create(new Date(999), true));

        verify(adPlayer).autopause(true);
    }

    @Test
    public void pausesInlayAdPlayerWhenPlayerIsPlayingForNoVideoOnScreenWithMuteFalse() {
        when(adPlayer.isPlaying()).thenReturn(true);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.AD_PLAYBACK, NoVideoOnScreen.create(new Date(999), false));

        verify(adPlayer).autopause(false);
    }

    @Test
    public void doesNotPauseInlayAdPlayerWhenPlayerNotPlayingForNoVideoOnScreen() {
        when(adPlayer.isPlaying()).thenReturn(false);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.AD_PLAYBACK, NoVideoOnScreen.create(new Date(999), true));

        verify(adPlayer, never()).autopause(anyBoolean());
    }

    @Test
    public void toggleMuteCommandForwardsCallToInlayAdPlayer() {
        final VideoAd videoAd = AdFixtures.getInlayVideoAd(1L);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.AD_PLAYBACK, InlayAdEvent.forToggleVolume(0, videoAd, new Date(999)));

        verify(adPlayer).toggleVolume();
    }

    @Test
    public void doesNotDoAnythingWithStateTransitions() {
        final VideoAd videoAd = AdFixtures.getInlayVideoAd(1L);
        final AdPlayStateTransition event = AdPlayStateTransition.create(videoAd, TestPlayerTransitions.idle(), false, new Date(999));

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.AD_PLAYBACK, event);

        verifyZeroInteractions(adPlayer);
        verifyZeroInteractions(inlayAdHelper);
    }

    @Test
    public void forwardsPlaybackToggleToInlayPlayer() {
        final VideoAd videoAd = AdFixtures.getInlayVideoAd(1L);

        operations.subscribe(inlayAdHelper);
        eventBus.publish(EventQueue.AD_PLAYBACK, InlayAdEvent.forTogglePlayback(0, videoAd, new Date(999)));

        verify(adPlayer).togglePlayback(videoAd);
    }
}
