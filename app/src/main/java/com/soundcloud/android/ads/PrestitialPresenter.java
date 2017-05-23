package com.soundcloud.android.ads;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import javax.inject.Inject;

class PrestitialPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements VisualPrestitialView.Listener,
            SponsoredSessionCardView.Listener {

    private final PrestitialAdsController adsController;

    private final AdViewabilityController adViewabilityController;
    private final PrestitialAdapterFactory prestitialAdapterFactory;
    private final Lazy<VisualPrestitialView> visualPrestitialView;
    private final Navigator navigator;
    private final EventBus eventBus;

    private Activity activity;
    private NoSwipeViewPager pager;

    @Inject
    PrestitialPresenter(PrestitialAdsController adsController,
                        AdViewabilityController adViewabilityController,
                        PrestitialAdapterFactory prestitialAdapterFactory,
                        Lazy<VisualPrestitialView> visualPrestitialView,
                        Navigator navigator,
                        EventBus eventBus) {
        this.adsController = adsController;
        this.adViewabilityController = adViewabilityController;
        this.prestitialAdapterFactory = prestitialAdapterFactory;
        this.visualPrestitialView = visualPrestitialView;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        final Optional<AdData> currentAd = adsController.getCurrentAd();
        if (currentAd.isPresent()) {
            this.activity = activity;
            bindView(currentAd.get(), activity);
        } else {
            activity.finish();
        }
    }

    private void bindView(AdData adData, AppCompatActivity activity) {
        if (adData instanceof SponsoredSessionAd) {
            activity.setContentView(R.layout.sponsored_session_prestitial);
            pager = (NoSwipeViewPager) activity.findViewById(R.id.prestitial_pager);
            pager.setAdapter(prestitialAdapterFactory.create((SponsoredSessionAd) adData, this));
        } else if (adData instanceof VisualPrestitialAd) {
            activity.setContentView(R.layout.visual_prestitial);
            visualPrestitialView.get().setupContentView(activity, (VisualPrestitialAd) adData, this);
        } else {
            activity.finish();
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        adViewabilityController.stopDisplayTracking();
        this.activity = null;
        pager = null;
    }

    //Visual Prestitial Listener
    @Override
    public void onClickThrough(View view, VisualPrestitialAd ad) {
        navigator.openAdClickthrough(view.getContext(), ad.clickthroughUrl());
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPrestitialAdClickThrough(ad));
        activity.finish();
    }

    @Override
    public void onImageLoadComplete(VisualPrestitialAd ad, View imageView) {
        adViewabilityController.startDisplayTracking(imageView, ad);
        eventBus.publish(EventQueue.TRACKING, PrestitialAdImpressionEvent.create(ad));
    }

    @Override
    public void onContinueClick() {
        activity.finish();
    }

    // Sponsored Session Listener
    @Override
    public void onOptInClick() {
        pager.setCurrentItem(1);
    }

    @Override
    public void onOptOutClick() {
        activity.finish();
    }
}
