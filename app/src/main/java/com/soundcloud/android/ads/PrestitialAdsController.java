package com.soundcloud.android.ads;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
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

    private final AdsOperations adsOperations;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final NavigationExecutor navigationExecutor;
    private final EventBus eventBus;

    private RootActivity activity;
    private Subscription subscriber = RxUtils.invalidSubscription();

    private Optional<AdData> currentAd = Optional.absent();

    @Inject
    PrestitialAdsController(AdsOperations adsOperations,
                            PlaySessionStateProvider playSessionStateProvider,
                            NavigationExecutor navigationExecutor,
                            EventBus eventBus) {
        this.adsOperations = adsOperations;
        this.playSessionStateProvider = playSessionStateProvider;
        this.navigationExecutor = navigationExecutor;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(RootActivity activity, @Nullable Bundle bundle) {
        super.onCreate(activity, bundle);
        this.activity = activity;

        if (bundle == null) {
            fetchPrestitialAdIfNecessary(activity.getIntent());
        }
    }

    @Override
    public void onNewIntent(RootActivity activity, Intent intent) {
        super.onNewIntent(activity, intent);
        fetchPrestitialAdIfNecessary(intent);
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

    private void fetchPrestitialAdIfNecessary(Intent intent) {
        final boolean startedFromLauncher = intent.getBooleanExtra(LauncherActivity.EXTRA_FROM_LAUNCHER, false);
        if (startedFromLauncher && !playSessionStateProvider.isPlaying()) {
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
            currentAd = Optional.of(data);    eventBus.publish(EventQueue.TRACKING, event);
                navigationExecutor.openPrestititalAd(activity);
        }
    }
}
