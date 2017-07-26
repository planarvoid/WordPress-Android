package com.soundcloud.android.ads;

import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PrestitialAdsController extends ActivityLightCycleDispatcher<RootActivity> {

    private final PlayerAdsController playerAdsController;
    private final StreamAdsController streamAdsController;

    private final AdsOperations adsOperations;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final Navigator navigator;
    private final AdsStorage adsStorage;
    private final EventBus eventBus;

    private RootActivity activity;
    private Subscription subscriber = RxUtils.invalidSubscription();

    private Optional<AdData> currentAd = Optional.absent();

    @Inject
    PrestitialAdsController(PlayerAdsController playerAdsController,
                            StreamAdsController streamAdsController,
                            AdsOperations adsOperations,
                            PlaySessionStateProvider playSessionStateProvider,
                            Navigator navigator,
                            AdsStorage adsStorage,
                            EventBus eventBus) {
        this.playerAdsController = playerAdsController;
        this.streamAdsController = streamAdsController;
        this.adsOperations = adsOperations;
        this.playSessionStateProvider = playSessionStateProvider;
        this.navigator = navigator;
        this.adsStorage = adsStorage;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(RootActivity activity, @Nullable Bundle bundle) {
        super.onCreate(activity, bundle);
        this.activity = activity;
    }

    @Override
    public void onNewIntent(RootActivity activity, Intent intent) {
        super.onNewIntent(activity, intent);
        fetchPrestitialAdIfNecessary(intent);
    }

    @Override
    public void onResume(RootActivity activity) {
        fetchPrestitialAdIfNecessary(activity.getIntent());
        super.onResume(activity);
    }

    @Override
    public void onPause(RootActivity activity) {
        subscriber.unsubscribe();
        super.onPause(activity);
    }

    @Override
    public void onDestroy(RootActivity activity) {
        currentAd = Optional.absent();
        this.activity = null;
        super.onDestroy(activity);
    }

    Optional<AdData> getCurrentAd() {
        return currentAd;
    }

    void clearAllExistingAds() {
        Log.d(Log.ADS_TAG, "Clearing all inserted ads");
        playerAdsController.clearAds();
        streamAdsController.clearAds();
    }

    private void fetchPrestitialAdIfNecessary(Intent intent) {
        final boolean startedFromLauncher = intent.getBooleanExtra(LauncherActivity.EXTRA_FROM_LAUNCHER, false);
        if ((startedFromLauncher || adsStorage.shouldShowPrestitial()) && !playSessionStateProvider.isPlaying()) {
            intent.putExtra(LauncherActivity.EXTRA_FROM_LAUNCHER, false);
            final AdRequestData requestData = AdRequestData.forPageAds(Optional.absent());
            subscriber = adsOperations.prestitialAd(requestData)
                                      .subscribe(new PrestitialAdSubscriber(requestData.getRequestId()));
        }
    }

    private class PrestitialAdSubscriber extends DefaultSubscriber<AdData> {
        private final String requestId;

        PrestitialAdSubscriber(String requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onNext(AdData data) {

            final AdDeliveryEvent event = AdDeliveryEvent.adDelivered(data.adUrn(), requestId);
            currentAd = Optional.of(data);
            eventBus.publish(EventQueue.TRACKING, event);
            navigator.navigateTo(NavigationTarget.forPrestitialAd());
        }
    }
}
