package com.soundcloud.android.ads;

import static com.soundcloud.android.ads.PrestitialAdapter.PrestitialPage;
import static com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import static com.soundcloud.android.events.AdPlaybackEvent.AdProgressEvent;
import static com.soundcloud.android.playback.VideoSurfaceProvider.Origin;

import com.soundcloud.android.R;
import com.soundcloud.android.events.AdPlaybackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.SponsoredSessionStartEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.PlaybackProgress;
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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

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

    private Optional<PrestitialPage> currentPage = Optional.absent();
    private Optional<SponsoredSessionPageListener> pageListener = Optional.absent();

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
            activity.setContentView(R.layout.sponsored_session_prestitial);
            bindSponsoredSession((SponsoredSessionAd) adData, activity);
        } else if (adData instanceof VisualPrestitialAd) {
            activity.setContentView(R.layout.visual_prestitial);
            visualPrestitialView.get().setupContentView(activity, (VisualPrestitialAd) adData, this);
        } else {
            activity.finish();
        }
    }

    private void bindSponsoredSession(SponsoredSessionAd ad, AppCompatActivity activity) {
        final PrestitialAdapter adapter = prestitialAdapterFactory.create(ad, this, sponsoredSessionVideoView.get());
        final ViewPager pager = activity.findViewById(R.id.prestitial_pager);
        final SponsoredSessionPageListener pageChangeListener = new SponsoredSessionPageListener(adapter, ad);
        pager.addOnPageChangeListener(pageChangeListener);
        pager.setAdapter(adapter);
        pagerRef = new WeakReference<>(pager);
        currentPage = Optional.of(PrestitialPage.OPT_IN_CARD);
        pageListener = Optional.of(pageChangeListener);
        eventBus.publish(EventQueue.TRACKING, PrestitialAdImpressionEvent.createForSponsoredSession(ad));
    }

    @Override
    public void onPause(AppCompatActivity host) {
        if (adPlayer.isPlaying()) {
            adPlayer.pause();
        }
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        adPlayer.getCurrentAd().ifPresent(ad -> {
            if (ad.monetizationType() == AdData.MonetizationType.SPONSORED_SESSION) {
                final SponsoredSessionVideoView videoCardView = sponsoredSessionVideoView.get();
                onVideoTextureBind(videoCardView.videoView, videoCardView.viewabilityLayer, ad);
            }
        });
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        cleanUpVideoResources(activity);
        adViewabilityController.stopDisplayTracking();
        currentPage = Optional.absent();
        pageListener = Optional.absent();
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

    private void endActivity() {
        ifRefPresent(activityRef, Activity::finish);
    }

    @Override
    public void onImageClick(Context context, AdData ad, Optional<PrestitialPage> page) {
        if (page.contains(PrestitialPage.OPT_IN_CARD))  {
            advanceToNextPage();
        } else {
            onClickThrough(((PrestitialAd) ad).clickthroughUrl(), ad);
        }
    }

    private void onClickThrough(Uri clickthroughUrl, AdData ad) {
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPrestitialAdClickThrough(ad));
        navigator.navigateTo(NavigationTarget.forAdClickthrough(clickthroughUrl.toString()));
        endActivity();
    }

    @Override
    public void onImageLoadComplete(AdData ad, View imageView, Optional<PrestitialPage> page) {
        if (ad instanceof VisualPrestitialAd) {
            adViewabilityController.startDisplayTracking(imageView, (VisualPrestitialAd) ad);
            publishTrackingEvent(PrestitialAdImpressionEvent.createForDisplay((VisualPrestitialAd) ad));
        } else if (ad instanceof SponsoredSessionAd && page.isPresent()) {
            onImageLoadForSponsoredSession((SponsoredSessionAd) ad, page.get());
        }
    }

    private void onImageLoadForSponsoredSession(SponsoredSessionAd ad, PrestitialPage page) {
        final PrestitialAdImpressionEvent event = PrestitialAdImpressionEvent.createForSponsoredSessionDisplay(ad, page.is(PrestitialPage.END_CARD));
        if (currentPage.contains(page)) {
            publishTrackingEvent(event);
        } else {
            pageListener.ifPresent(listener -> listener.addDeferredEventForPage(page, event));
        }
    }

    @Override
    public void onOptionOneClick(PrestitialPage page, SponsoredSessionAd ad, Context context) {
        if (page.is(PrestitialPage.OPT_IN_CARD)) {
            endActivity();
        } else if (page.is(PrestitialPage.END_CARD)) {
            onClickThrough(ad.clickthroughUrl(), ad);
        }
    }

    @Override
    public void onOptionTwoClick(PrestitialPage page, SponsoredSessionAd ad) {
        if (page.is(PrestitialPage.OPT_IN_CARD)) {
            advanceToNextPage();
        } else if (page.is(PrestitialPage.END_CARD)) {
            endActivity();
        }
    }

    @Override
    public void onContinueClick() {
        endActivity();
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
    public void onSkipAd() {
        adPlayer.pause();
        advanceToNextPage();
    }

    @Override
    public void onWhyAdsClicked(Context context) {
        whyAdsDialogPresenter.show(context);
    }

    private void publishTrackingEvent(TrackingEvent event) {
        eventBus.publish(EventQueue.TRACKING, event);
    }

    private <T> void ifRefPresent(WeakReference<T> reference, Consumer<T> consumer) {
        if (reference != null && reference.get() != null) {
            consumer.accept(reference.get());
        }
    }

    private class SponsoredSessionPageListener extends ViewPager.SimpleOnPageChangeListener {

        private final PrestitialAdapter adapter;
        private final SponsoredSessionAd ad;

        Map<PrestitialPage, TrackingEvent> deferredEvent = new HashMap<>(PrestitialPage.values().length);

        SponsoredSessionPageListener(PrestitialAdapter adapter, SponsoredSessionAd ad) {
            this.adapter = adapter;
            this.ad = ad;
        }

        @Override
        public void onPageSelected(int position) {
            currentPage = Optional.of(adapter.getPage(position));
            publishDeferredEvents(currentPage.get());
            if (currentPage.contains(PrestitialPage.VIDEO_CARD)) {
                onVideoPageSelected();
            } else if (currentPage.contains(PrestitialPage.END_CARD)) {
                onEndCardSelected();
            }
        }

        private void onVideoPageSelected() {
            sponsoredSessionVideoView.get().adjustLayoutForVideo(ad.video());
            adPlayer.play(ad.video(), true);
            subscription = eventBus.queue(EventQueue.AD_PLAYBACK)
                                   .filter(event -> event.forStateTransition() || event.forAdProgressEvent())
                                   .subscribe(new AdTransitionSubscriber());
        }

        void addDeferredEventForPage(PrestitialPage page, TrackingEvent event) {
            deferredEvent.put(page, event);
        }

        private void publishDeferredEvents(PrestitialPage page) {
            if (deferredEvent.containsKey(page)) {
                publishTrackingEvent(deferredEvent.get(page));
                deferredEvent.remove(page);
            }
        }

        private void onEndCardSelected() {
            eventBus.publish(EventQueue.TRACKING, SponsoredSessionStartEvent.create(ad, Screen.PRESTITIAL));
            adsController.clearAllExistingAds();
        }
    }

    private class AdTransitionSubscriber extends DefaultSubscriber<AdPlaybackEvent> {
        @Override
        public void onNext(AdPlaybackEvent event) {
            final SponsoredSessionVideoView videoView = PrestitialPresenter.this.sponsoredSessionVideoView.get();
            if (event.forAdProgressEvent()) {
                final PlaybackProgress progress = ((AdProgressEvent) event).playbackProgress();
                videoView.setProgress(progress);
            } else if (event.forStateTransition()) {
                final PlaybackStateTransition stateTransition = ((AdPlayStateTransition) event).stateTransition();
                videoView.setPlayState(stateTransition);
                if (stateTransition.playbackEnded() || stateTransition.wasError()) {
                    advanceToNextPage();
                }
            }
        }
    }
}
