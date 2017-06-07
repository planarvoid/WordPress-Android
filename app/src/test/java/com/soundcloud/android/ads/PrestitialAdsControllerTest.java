package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;

public class PrestitialAdsControllerTest extends AndroidUnitTest {

    @Mock PlayerAdsController playerAdsController;
    @Mock StreamAdsController streamAdsController;

    @Mock NavigationExecutor navigationExecutor;
    @Mock AdsOperations adsOperations;
    @Mock PlaySessionStateProvider playSessionStateProvider;
    @Mock AdsStorage adsStorage;

    @Mock RootActivity activity;
    @Mock Intent intent;

    private TestEventBus eventBus;
    private PrestitialAdsController controller;
    private TestPrestitialStateSetup stateSetup;
    private final Observable<VisualPrestitialAd> VISUAL_PRESTITIAL_AD = Observable.just(AdFixtures.visualPrestitialAd());
    private final Observable<VisualPrestitialAd> NO_AD = Observable.empty();
    private Runnable notFetched, notOpened, isFetched, isOpened;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        controller = new PrestitialAdsController(playerAdsController,
                                                 streamAdsController,
                                                 adsOperations,
                                                 playSessionStateProvider,
                                                 navigationExecutor,
                                                 adsStorage,
                                                 eventBus);

        when(activity.getIntent()).thenReturn(intent);
        stateSetup = new TestPrestitialStateSetup();
        setUpVerifications();
        controller.onCreate(activity, null);
    }

    @Test
    public void willNotFetchPrestitialOnResumeIfNotLaunchedFromLauncher() {
        stateSetup.isPlaying(false)
                  .fromLauncher(false)
                  .hasPassedTimeLimit(false)
                  .withAd(VISUAL_PRESTITIAL_AD);

        controller.onResume(activity);

        verifyPrestitial(notFetched, notOpened);
    }

    @Test
    public void willNotFetchPrestitialOnResumeIfLaunchedFromLauncherAndPlayerPlaying() {
        stateSetup.isPlaying(true)
                  .fromLauncher(true)
                  .hasPassedTimeLimit(false)
                  .withAd(VISUAL_PRESTITIAL_AD);

        controller.onResume(activity);

        verifyPrestitial(notFetched, notOpened);
    }

    @Test
    public void willFetchAndLaunchPrestitialOnCreateIfLaunchedFromLauncherAndPlayerNotPlaying() {
        stateSetup.isPlaying(false)
                  .fromLauncher(true)
                  .hasPassedTimeLimit(false)
                  .withAd(VISUAL_PRESTITIAL_AD);

        controller.onResume(activity);

        verifyPrestitial(isFetched, isOpened);
    }

    @Test
    public void willFetchAndLaunchPrestitialOnResumeIfTimeLimitHasPassedAndPlayerNotPlaying() {
        stateSetup.isPlaying(false)
                  .fromLauncher(false)
                  .hasPassedTimeLimit(true)
                  .withAd(VISUAL_PRESTITIAL_AD);

        controller.onResume(activity);

        verifyPrestitial(isFetched, isOpened);
    }

    @Test
    public void willNotFetchAndLaunchPrestitialOnResumeIfTimeLimitHasPassedAndPlayerIsPlaying() {
        stateSetup.isPlaying(true)
                  .fromLauncher(false)
                  .hasPassedTimeLimit(true)
                  .withAd(VISUAL_PRESTITIAL_AD);

        controller.onResume(activity);

        verifyPrestitial(notFetched, notOpened);
    }

    @Test
    public void willNotLaunchPrestitialIfPrestitialsFetchReturnsEmpty() {
        stateSetup.isPlaying(false)
                  .fromLauncher(true)
                  .hasPassedTimeLimit(true)
                  .withAd(NO_AD);

        controller.onResume(activity);

        verifyPrestitial(isFetched, notOpened);
    }

    @Test
    public void willNotFetchPrestitialOnNewIntentIfNotLaunchedFromLauncher() {
        stateSetup.isPlaying(false)
                  .fromLauncher(false)
                  .hasPassedTimeLimit(false)
                  .withAd(VISUAL_PRESTITIAL_AD);

        controller.onNewIntent(activity, intent);

        verifyPrestitial(notFetched, notOpened);
    }

    @Test
    public void willNotFetchPrestitialOnNewIntentIfLaunchedFromLauncherAndPlayerPlaying() {
        stateSetup.isPlaying(true)
                  .fromLauncher(true)
                  .hasPassedTimeLimit(false)
                  .withAd(VISUAL_PRESTITIAL_AD);

        controller.onNewIntent(activity, intent);

        verifyPrestitial(notFetched, notOpened);
    }

    @Test
    public void willFetchAndLaunchPrestitialOnNewIntentIfLaunchedFromLauncherAndPlayerNotPlaying() {
        stateSetup.isPlaying(false)
                  .fromLauncher(true)
                  .hasPassedTimeLimit(false)
                  .withAd(VISUAL_PRESTITIAL_AD);

        controller.onNewIntent(activity, intent);

        verifyPrestitial(isFetched, isOpened);
    }

    @Test
    public void clearAllExistingAdsShouldCallClearAdsOnPlayerAdsControllerAndStreamAdsController() {
        controller.clearAllExistingAds();

        verify(playerAdsController).clearAds();
        verify(streamAdsController).clearAds();
    }

    private void setUpVerifications() {
        isFetched = () -> verify(adsOperations, times(1)).prestitialAd(any(AdRequestData.class));
        notFetched = () -> verify(adsOperations, never()).prestitialAd(any(AdRequestData.class));
        isOpened = () -> {
            verify(navigationExecutor, times(1)).openPrestititalAd(activity);
            assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(AdDeliveryEvent.class);
        };
        notOpened = () -> {
            verify(navigationExecutor, never()).openPrestititalAd(activity);
            assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
        };
    }

    private void verifyPrestitial(Runnable... verifications) {
        for(Runnable verification : verifications) {
            verification.run();
        }
    }

    private class TestPrestitialStateSetup {
        TestPrestitialStateSetup fromLauncher(boolean isFromLauncher) {
            when(intent.getBooleanExtra(LauncherActivity.EXTRA_FROM_LAUNCHER, false)).thenReturn(isFromLauncher);
            return this;
        }

        TestPrestitialStateSetup withAd(Observable ad) {
            when(adsOperations.prestitialAd(any(AdRequestData.class))).thenReturn(ad);
            return this;
        }

        TestPrestitialStateSetup isPlaying(boolean isPlaying) {
            when(playSessionStateProvider.isPlaying()).thenReturn(isPlaying);
            return this;
        }

        TestPrestitialStateSetup hasPassedTimeLimit(boolean canRequestPrestitial) {
            when(adsStorage.shouldShowPrestitial()).thenReturn(canRequestPrestitial);
            return this;
        }
    }
}
