package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.PlayerSwipeToSkipExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;

import javax.inject.Inject;

class PlayerPagerOnboardingPresenter extends DefaultSupportFragmentLightCycle<PlayerFragment> {
    private final PlayerSwipeToSkipExperiment experiment;
    private final EventBus eventBus;

    private PlayerTrackPager pager;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    PlayerPagerOnboardingPresenter(PlayerSwipeToSkipExperiment experiment, EventBus eventBus) {
        this.experiment = experiment;
        this.eventBus = eventBus;
    }

    @Override
    public void onViewCreated(PlayerFragment fragment, View view, Bundle savedInstanceState) {
        pager = (PlayerTrackPager) view.findViewById(R.id.player_track_pager);
    }

    @Override
    public void onResume(PlayerFragment fragment) {
        subscription = eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerExtendedSubscriber());
    }

    @Override
    public void onPause(PlayerFragment fragment) {
        subscription.unsubscribe();
    }

    private void showOnboarding() {
        if (isExperimentEnabled()) {
            start(pager);
        }
    }

    private boolean isExperimentEnabled() {
        return experiment.isEnabled();
    }

    private void start(PlayerTrackPager pager) {
        start(pager, 50, 350, 0.88f, 350);
    }

    private void start(ViewPager pager,
                       int distanceDp,
                       int duration,
                       float accelerationFactor,
                       int holdOnTime) {
        final NudgedView view = new NudgedView(pager);
        final Context context = pager.getContext();
        if (view.isAvailable() && view.start()) {
            final int distancePx = dpToPixel(context, distanceDp);
            show(context,
                 "Duration: " + duration + "," +
                         "Distance: " + distanceDp + "dp (" + distancePx + " px)" + "," +
                         "Acceleration: " + accelerationFactor + "," +
                         "Hold on ime (ms): " + holdOnTime
            );

            final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, distancePx);

            final TimeInterpolator value = new AccelerateInterpolator(accelerationFactor);
            valueAnimator.setInterpolator(value);
            valueAnimator.setDuration(duration);
            valueAnimator.addUpdateListener(new MyAnimatorUpdateListener(view));
            valueAnimator.addListener(new MyAnimatorListener(view, holdOnTime));
            valueAnimator.start();
        } else {
            show(context, "Failed to start fake drag");
        }
    }

    private static int dpToPixel(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private static void show(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private static class MyAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private final NudgedView view;

        private MyAnimatorUpdateListener(NudgedView view) {
            this.view = view;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            final float currentValue = (float) valueAnimator.getAnimatedValue();
            view.move(-currentValue);
        }
    }

    private static class MyAnimatorListener implements Animator.AnimatorListener {
        private final NudgedView view;
        private final int timeBeforeDeceleration;

        private MyAnimatorListener(NudgedView view, int timeBeforeDeceleration) {
            this.view = view;
            this.timeBeforeDeceleration = timeBeforeDeceleration;
        }

        @Override
        public void onAnimationStart(Animator animator) {
            view.start();
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.stop();

                }
            }, timeBeforeDeceleration);
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            view.stop();
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            // no-op
        }
    }

    private static class NudgedView {
        private final ViewPager pager;

        private float lastPosition;

        NudgedView(ViewPager pager) {
            this.pager = pager;
        }

        boolean isAvailable() {
            return !pager.isFakeDragging();
        }

        boolean start() {
            this.lastPosition = 0;
            return pager.beginFakeDrag();
        }

        void move(float nextPositionPx) {
            final float distance = nextPositionPx - lastPosition;
            System.out.println("fakeDragBy: distance:" + distance + " nextPositionPx = [" + nextPositionPx + "]");
            pager.fakeDragBy(distance);
            lastPosition = nextPositionPx;
        }

        void stop() {
            pager.endFakeDrag();
        }
    }

    private class PlayerExtendedSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                showOnboarding();
            }
        }
    }
}
