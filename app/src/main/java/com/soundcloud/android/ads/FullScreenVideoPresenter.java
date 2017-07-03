package com.soundcloud.android.ads;

import static com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import static com.soundcloud.android.events.AdPlaybackEvent.InlayAdEvent;

import com.soundcloud.android.Consts;
import com.soundcloud.android.events.AdPlaybackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

class FullScreenVideoPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements FullScreenVideoView.Listener {

    private final FullScreenVideoView view;
    private final AdPlayer adPlayer;
    private final AdStateProvider stateProvider;
    private final CurrentDateProvider dateProvider;
    private final EventBus eventBus;
    private final TrackSourceInfo trackSourceInfo;
    private final StreamAdsController streamAdsController;
    private final AdViewabilityController adViewabilityController;
    private final Navigator navigator;

    private Optional<VideoAd> ad = Optional.absent();
    private Subscription subscription = RxUtils.invalidSubscription();

    @Nullable private WeakReference<Activity> activityRef;

    @Inject
    FullScreenVideoPresenter(FullScreenVideoView view,
                             AdViewabilityController adViewabilityController,
                             AdStateProvider stateProvider,
                             StreamAdsController streamAdsController,
                             CurrentDateProvider dateProvider,
                             AdPlayer adPlayer,
                             EventBus eventBus,
                             Navigator navigator) {
        this.navigator = navigator;
        view.setListener(this);
        this.view = view;
        this.adPlayer = adPlayer;
        this.stateProvider = stateProvider;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
        this.streamAdsController = streamAdsController;
        this.adViewabilityController = adViewabilityController;
        this.trackSourceInfo = new TrackSourceInfo(Screen.VIDEO_FULLSCREEN.get(), true);
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        final Bundle extras = activity.getIntent().getExtras();
        if (extras.containsKey(FullScreenVideoActivity.EXTRA_AD_URN)) {
            ad = adPlayer.getCurrentAd();
            activityRef = new WeakReference<>(activity);
            bindView(Urns.urnFromBundle(extras, FullScreenVideoActivity.EXTRA_AD_URN), activity);
        } else {
            activity.finish();
        }
    }

    private void bindView(Urn urn, AppCompatActivity activity) {
        if (ad.isPresent() && ad.get().adUrn().equals(urn)) {
            final VideoAd video = ad.get();
            onScreenSizeChange(video, true);
            view.setupContentView(activity, video);
            stateProvider.get(video.uuid()).ifPresent(event -> onInlayStateTransition(activity, event));
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromVideoAdFullscreen(video, trackSourceInfo));
        } else {
            activity.finish();
        }
    }

    private void onInlayStateTransition(Activity activity, AdPlayStateTransition event) {
        final PlaybackStateTransition transition = event.stateTransition();
        if (transition.playbackHasStopped()) {
            activity.finish();
        } else {
            view.setPlayState(transition);
        }
    }

    private boolean hasActivityRef() {
        return activityRef != null && activityRef.get() != null;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        ad.ifPresent(videoAd -> view.bindVideoSurface(videoAd.uuid(), VideoSurfaceProvider.Origin.FULLSCREEN));
        subscription = eventBus.queue(EventQueue.AD_PLAYBACK)
                               .filter(AdPlaybackEvent::forStateTransition)
                               .cast(AdPlayStateTransition.class)
                               .subscribe(new TransitionSubscriber());
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        streamAdsController.setFullscreenDisabled();
        subscription.unsubscribe();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        ad.ifPresent(video -> {
            onScreenSizeChange(video, false);
            view.unbindVideoSurface(VideoSurfaceProvider.Origin.FULLSCREEN);
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromVideoAdShrink(video, trackSourceInfo));
            ad = Optional.absent();
        });
    }

    private void onScreenSizeChange(VideoAd ad, boolean isFullscreen) {
        adPlayer.lastPosition(ad).ifPresent(progress -> adViewabilityController.onScreenSizeChange(ad, isFullscreen, progress.getPosition()));
    }

    @Override
    public void onTogglePlayClick() {
        ad.ifPresent(video -> eventBus.publish(EventQueue.AD_PLAYBACK,
                                               InlayAdEvent.forTogglePlayback(Consts.NOT_SET, video, dateProvider.getCurrentDate())));
    }

    @Override
    public void onShrinkClick() {
        if (hasActivityRef()) {
            activityRef.get().finish();
        }
    }

    @Override
    public void onLearnMoreClick(Context context) {
        ad.ifPresent(video -> {
            navigator.navigateTo(ViewUtils.getFragmentActivity(context), NavigationTarget.forAdClickthrough(video.clickThroughUrl()));
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayableClickThrough(video, trackSourceInfo));
        });
    }

    private class TransitionSubscriber extends DefaultSubscriber<AdPlayStateTransition> {
        @Override
        public void onNext(AdPlayStateTransition event) {
            if (hasActivityRef()) {
                onInlayStateTransition(activityRef.get(), event);
            }
        }
    }
}
