package com.soundcloud.android.ads;

import static com.soundcloud.android.playback.VideoSurfaceProvider.Origin;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.events.AdPlaybackEvent;
import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.functions.Consumer;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import rx.Subscription;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

class PrestitialPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements PrestitialView.Listener {

    private final PrestitialAdsController adsController;
    private final AdViewabilityController adViewabilityController;
    private final PrestitialAdapterFactory prestitialAdapterFactory;

    private final Lazy<VisualPrestitialView> visualPrestitialView;
    private final Lazy<SponsoredSessionVideoView> sponsoredSessionVideoView;

    private final VideoSurfaceProvider videoSurfaceProvider;
    private final WhyAdsDialogPresenter whyAdsDialogPresenter;
    private final AdPlayer adPlayer;
    private final Navigator navigator;
    private final EventBus eventBus;

    private WeakReference<Activity> activityRef;
    private WeakReference<ViewPager> pagerRef;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    PrestitialPresenter(PrestitialAdsController adsController,
                        AdViewabilityController adViewabilityController,
                        PrestitialAdapterFactory prestitialAdapterFactory,
                        Lazy<VisualPrestitialView> visualPrestitialView,
                        Lazy<SponsoredSessionVideoView> sponsoredSessionVideoView,
                        VideoSurfaceProvider videoSurfaceProvider,
                        WhyAdsDialogPresenter whyAdsDialogPresenter,
                        AdPlayer adPlayer,
                        Navigator navigator,
                        EventBus eventBus) {
        this.adsController = adsController;
        this.adViewabilityController = adViewabilityController;
        this.prestitialAdapterFactory = prestitialAdapterFactory;
        this.visualPrestitialView = visualPrestitialView;
        this.sponsoredSessionVideoView = sponsoredSessionVideoView;
        this.videoSurfaceProvider = videoSurfaceProvider;
        this.whyAdsDialogPresenter = whyAdsDialogPresenter;
        this.adPlayer = adPlayer;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        final Optional<AdData> currentAd = adsController.getCurrentAd();
        if (currentAd.isPresent()) {
            this.activityRef = new WeakReference<>(activity);
            bindView(currentAd.get(), activity);
        } else {
            activity.finish();
        }
    }

    private void bindView(AdData adData, AppCompatActivity activity) {
        if (adData instanceof SponsoredSessionAd) {
            bindSponsoredSession((SponsoredSessionAd) adData, activity);
        } else if (adData instanceof VisualPrestitialAd) {
            activity.setContentView(R.layout.visual_prestitial);
            visualPrestitialView.get().setupContentView(activity, (VisualPrestitialAd) adData, this);
        } else {
            activity.finish();
        }
    }

    private void bindSponsoredSession(SponsoredSessionAd ad, AppCompatActivity activity) {
        activity.setContentView(R.layout.sponsored_session_prestitial);

        final PrestitialAdapter adapter = prestitialAdapterFactory.create(ad, this, sponsoredSessionVideoView.get());
        final ViewPager pager = (ViewPager) activity.findViewById(R.id.prestitial_pager);
        pager.addOnPageChangeListener(new SponsoredSessionPageListener(adapter, ad.video()));
        pager.setAdapter(adapter);
        pagerRef = new WeakReference<>(pager);
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        adPlayer.getCurrentAd().ifPresent(ad -> {
            final SponsoredSessionVideoView videoCardView = sponsoredSessionVideoView.get();
            onVideoTextureBind(videoCardView.videoView, videoCardView.viewabilityLayer, ad);
        });
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        cleanUpVideoResources(activity);
        adViewabilityController.stopDisplayTracking();
    }

    private void cleanUpVideoResources(AppCompatActivity activity) {
        if (activity.isChangingConfigurations()) {
            videoSurfaceProvider.onConfigurationChange(Origin.PRESTITIAL);
        } else {
            subscription.unsubscribe();
            videoSurfaceProvider.onDestroy(Origin.PRESTITIAL);
            adPlayer.reset();
        }
    }

    private void advanceToNextPage() {
        ifRefPresent(pagerRef, pager -> {
            final int nextItem = pager.getCurrentItem() + 1;
            if (nextItem < pager.getAdapter().getCount()) {
                pager.setCurrentItem(nextItem);
            }
        });
    }

    @Override
    public void onClickThrough(View view, AdData ad) {
        if (ad instanceof VisualPrestitialAd) {
            final VisualPrestitialAd visualAd = (VisualPrestitialAd) ad;
            navigator.openAdClickthrough(view.getContext(), visualAd.clickthroughUrl());
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPrestitialAdClickThrough(visualAd));
            closePrestitial();
        }
    }

    @Override
    public void onImageLoadComplete(AdData ad, View imageView) {
        if (ad instanceof VisualPrestitialAd) {
            final VisualPrestitialAd visualAd = (VisualPrestitialAd) ad;
            adViewabilityController.startDisplayTracking(imageView, visualAd);
            eventBus.publish(EventQueue.TRACKING, PrestitialAdImpressionEvent.create(visualAd));
        }
    }

    @Override
    public void closePrestitial() {
        ifRefPresent(activityRef, Activity::finish);
    }

    @Override
    public void onOptInClick() {
        advanceToNextPage();
    }

    @Override
    public void onTogglePlayback() {
        adPlayer.getCurrentAd().ifPresent(adPlayer::togglePlayback);
    }

    @Override
    public void onVideoTextureBind(TextureView textureView, View viewabilityLayer, VideoAd videoAd) {
        videoSurfaceProvider.setTextureView(videoAd.uuid(), Origin.PRESTITIAL, textureView, viewabilityLayer);
    }

    @Override
    public void onWhyAdsClicked(Context context) {
        whyAdsDialogPresenter.show(context);
    }

    private <T> void ifRefPresent(WeakReference<T> reference, Consumer<T> consumer) {
        if (reference != null && reference.get() != null) {
            consumer.accept(reference.get());
        }
    }

    private class SponsoredSessionPageListener extends ViewPager.SimpleOnPageChangeListener {

        private final PrestitialAdapter adapter;
        private final VideoAd videoAd;

        SponsoredSessionPageListener(PrestitialAdapter adapter, VideoAd videoAd) {
            this.adapter = adapter;
            this.videoAd = videoAd;
        }

        @Override
        public void onPageSelected(int position) {
            switch (adapter.getPage(position)) {
                case VIDEO_CARD:
                    onVideoPageSelect();
                    break;
                default:
                    break;
            }
        }

        private void onVideoPageSelect() {
            sponsoredSessionVideoView.get().adjustLayoutForVideo(videoAd);
            adPlayer.play(videoAd, true);
            subscription = eventBus.queue(EventQueue.INLAY_AD)
                                   .filter(AdPlaybackEvent::forStateTransition)
                                   .cast(AdPlayStateTransition.class)
                                   .subscribe(new AdTransitionSubscriber());
        }
    }

    private class AdTransitionSubscriber extends DefaultSubscriber<AdPlayStateTransition> {
        @Override
        public void onNext(AdPlayStateTransition transition) {
            final PlaybackStateTransition stateTransition = transition.stateTransition();
            sponsoredSessionVideoView.get().setPlayState(stateTransition);
            if (stateTransition.playbackEnded()) {
                advanceToNextPage();
            }
        }
    }
}
