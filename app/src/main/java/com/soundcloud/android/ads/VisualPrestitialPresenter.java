package com.soundcloud.android.ads;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import javax.inject.Inject;

class VisualPrestitialPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements VisualPrestitialView.Listener {

    private final VisualPrestitialView view;
    private final AdViewabilityController adViewabilityController;
    private final Navigator navigator;
    private final EventBus eventBus;

    @Inject
    VisualPrestitialPresenter(VisualPrestitialView view,
                              AdViewabilityController adViewabilityController,
                              Navigator navigator,
                              EventBus eventBus) {
        this.view = view;
        this.adViewabilityController = adViewabilityController;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        final Bundle extras = activity.getIntent().getExtras();
        if (extras.containsKey(VisualPrestitialActivity.EXTRA_AD)) {
            Optional<AdData> ad = Optional.of(extras.getParcelable(VisualPrestitialActivity.EXTRA_AD));
            bindView(ad.get(), activity);
        } else {
            activity.finish();
        }
    }

    private void bindView(AdData adData, AppCompatActivity activity) {
        if (adData instanceof VisualPrestitialAd) {
            VisualPrestitialAd  ad = (VisualPrestitialAd) adData;
            view.setupContentView(activity, ad, this);
        } else {
            activity.finish();
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        adViewabilityController.stopDisplayTracking();
    }

    @Override
    public void onClickThrough(AppCompatActivity activity, View view, VisualPrestitialAd ad) {
        navigator.openAdClickthrough(view.getContext(), ad.clickthroughUrl());
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPrestitialAdClickThrough(ad));
        activity.finish();
    }

    @Override
    public void onImageLoadComplete(VisualPrestitialAd ad, View imageView) {
        adViewabilityController.startDisplayTracking(imageView, ad);
        eventBus.publish(EventQueue.TRACKING, PrestitialAdImpressionEvent.create(ad));
    }

}
