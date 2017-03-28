package com.soundcloud.android.ads;

import static com.soundcloud.android.events.InlayAdEvent.InlayPlayStateTransition;
import static com.soundcloud.android.events.InlayAdEvent.TogglePlayback;

import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

class FullScreenVideoPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements FullScreenVideoView.Listener {

    private final FullScreenVideoView view;
    private final InlayAdPlayer adPlayer;
    private final InlayAdStateProvider stateProvider;
    private final CurrentDateProvider dateProvider;
    private final EventBus eventBus;
    private final Navigator navigator;
    private final TrackSourceInfo trackSourceInfo;
    private final StreamAdsController streamAdsController;

    private Optional<VideoAd> ad = Optional.absent();
    private Subscription subscription = RxUtils.invalidSubscription();

    @Nullable private WeakReference<Activity> activityRef;

    @Inject
    FullScreenVideoPresenter(FullScreenVideoView view,
                             InlayAdPlayer adPlayer,
                             InlayAdStateProvider stateProvider,
                             StreamAdsController streamAdsController,
                             CurrentDateProvider dateProvider,
                             Navigator navigator,
                             EventBus eventBus) {
        view.setListener(this);
        this.view = view;
        this.adPlayer = adPlayer;
        this.stateProvider = stateProvider;
        this.dateProvider = dateProvider;
        this.navigator = navigator;
        this.eventBus = eventBus;
        this.streamAdsController = streamAdsController;
        this.trackSourceInfo = new TrackSourceInfo(Screen.VIDEO_FULLSCREEN.get(), true);
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        final Bundle extras = activity.getIntent().getExtras();
        if (extras.containsKey(FullScreenVideoActivity.EXTRA_AD_URN)) {
            ad = adPlayer.getCurrentAd();
            activityRef = new WeakReference<>(activity);
            streamAdsController.onScreenSizeChange(true);
            bindView((Urn) extras.get(FullScreenVideoActivity.EXTRA_AD_URN), activity);
        } else {
            activity.finish();
        }
    }

    private void bindView(Urn urn, AppCompatActivity activity) {
        if (ad.isPresent() && ad.get().getAdUrn().equals(urn)) {
            final VideoAd video = ad.get();
            view.setupContentView(activity, video);
            stateProvider.get(video.getUuid()).ifPresent(event -> onInlayStateTransition(activity, event));
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromVideoAdFullscreen(video, trackSourceInfo));
        } else {
            activity.finish();
        }
    }

    private void onInlayStateTransition(Activity activity, InlayPlayStateTransition event) {
        final PlaybackStateTransition transition = event.stateTransition();
        if (transition.playbackHasStopped())  {
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
        ad.ifPresent(videoAd -> view.bindVideoSurface(videoAd.getUuid(), VideoSurfaceProvider.Origin.FULLSCREEN));
        subscription = eventBus.queue(EventQueue.INLAY_AD)
                               .filter(InlayAdEvent::forStateTransition)
                               .cast(InlayPlayStateTransition.class)
                               .subscribe(new TransitionSubscriber());
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        streamAdsController.onScreenSizeChange(false);
        subscription.unsubscribe();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        ad.ifPresent(video -> {
            view.unbindVideoSurface(VideoSurfaceProvider.Origin.FULLSCREEN);
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromVideoAdShrink(video, trackSourceInfo));
            ad = Optional.absent();
        });
    }

    @Override
    public void onTogglePlayClick() {
        ad.ifPresent(video -> eventBus.publish(EventQueue.INLAY_AD,
                                               TogglePlayback.create(Consts.NOT_SET, video, dateProvider.getCurrentDate())));
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
            navigator.openAdClickthrough(context, Uri.parse(video.getClickThroughUrl()));
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayableClickThrough(video, trackSourceInfo));
        });
    }

    private class TransitionSubscriber extends DefaultSubscriber<InlayPlayStateTransition> {
        @Override
        public void onNext(InlayPlayStateTransition event) {
            if (hasActivityRef()) {
               onInlayStateTransition(activityRef.get(), event);
            }
        }
    }
}
