package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.content.Intent;

public class PrestitialAdsControllerTest extends AndroidUnitTest {

    @Mock NavigationExecutor navigationExecutor;
    @Mock AdsOperations adsOperations;
    @Mock PlaySessionStateProvider playSessionStateProvider;

    @Mock RootActivity activity;
    @Mock Intent intent;

    private TestEventBus eventBus;
    private PrestitialAdsController controller;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        controller = new PrestitialAdsController(adsOperations,
                                                 playSessionStateProvider,
                                                 navigationExecutor,
                                                 eventBus);

        when(activity.getIntent()).thenReturn(intent);
    }

    @Test
    public void willNotFetchPrestitialOnCreateIfNotLaunchedFromLauncher() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        controller.onCreate(activity, null);

        verify(adsOperations, never()).prestitialAd(any(AdRequestData.class));
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void willNotFetchPrestitialOnCreateIfLaunchedFromLauncherAndPlayerPlaying() {
        final VisualPrestitialAd prestitial = AdFixtures.visualPrestitialAd();
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(intent.getBooleanExtra(LauncherActivity.EXTRA_FROM_LAUNCHER, false)).thenReturn(true);
        when(adsOperations.prestitialAd(any(AdRequestData.class))).thenReturn(Observable.just(prestitial));

        controller.onCreate(activity, null);

        verify(adsOperations, never()).prestitialAd(any(AdRequestData.class));
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void willFetchAndLaunchPrestitialOnCreateIfLaunchedFromLauncherAndPlayerNotPlaying() {
        final VisualPrestitialAd prestitial = AdFixtures.visualPrestitialAd();
        when(playSessionStateProvider.isPlaying()).thenReturn(false);
        when(intent.getBooleanExtra(LauncherActivity.EXTRA_FROM_LAUNCHER, false)).thenReturn(true);
        when(adsOperations.prestitialAd(any(AdRequestData.class))).thenReturn(Observable.just(prestitial));

        controller.onCreate(activity, null);

        verify(navigationExecutor).openPrestititalAd(activity);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(AdDeliveryEvent.class);
    }

    @Test
    public void willNotLaunchPrestitialIfPrestitialsFetchReturnsEmpty() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);
        when(intent.getBooleanExtra(LauncherActivity.EXTRA_FROM_LAUNCHER, false)).thenReturn(true);
        when(adsOperations.prestitialAd(any(AdRequestData.class))).thenReturn(Observable.empty());

        controller.onCreate(activity, null);

        verify(navigationExecutor, never()).openPrestititalAd(any(Context.class));
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void willNotFetchPrestitialOnNewIntentIfNotLaunchedFromLauncher() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        controller.onNewIntent(activity, intent);

        verify(adsOperations, never()).prestitialAd(any(AdRequestData.class));
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void willNotFetchPrestitialOnNewIntentIfLaunchedFromLauncherAndPlayerPlaying() {
        final VisualPrestitialAd prestitial = AdFixtures.visualPrestitialAd();
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(intent.getBooleanExtra(LauncherActivity.EXTRA_FROM_LAUNCHER, false)).thenReturn(true);
        when(adsOperations.prestitialAd(any(AdRequestData.class))).thenReturn(Observable.just(prestitial));

        controller.onNewIntent(activity, intent);

        verify(adsOperations, never()).prestitialAd(any(AdRequestData.class));
        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void willFetchAndLaunchPrestitialOnNewIntentIfLaunchedFromLauncherAndPlayerNotPlaying() {
        final VisualPrestitialAd prestitial = AdFixtures.visualPrestitialAd();

        controller.onCreate(activity, null);
        when(playSessionStateProvider.isPlaying()).thenReturn(false);
        when(intent.getBooleanExtra(LauncherActivity.EXTRA_FROM_LAUNCHER, false)).thenReturn(true);
        when(adsOperations.prestitialAd(any(AdRequestData.class))).thenReturn(Observable.just(prestitial));

        controller.onNewIntent(activity, intent);

        verify(navigationExecutor, times(1)).openPrestititalAd(activity);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(AdDeliveryEvent.class);
    }
}
