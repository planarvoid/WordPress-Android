package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.utils.ViewUtils.dpToPx;

import com.soundcloud.android.configuration.experiments.PlayerSwipeToSkipExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.view.animation.AccelerateInterpolator;

import javax.inject.Inject;

class PlayerPagerOnboardingPresenter extends DefaultSupportFragmentLightCycle<PlayerFragment> {
    private static final String TAG = "PlayerPagerOnboardingPresenter";

    private static final int MAX_ONBOARDING_RUN = 3;
    private static final int ANIMATION_IN_DURATION_MS = 350;
    private static final int ANIMATION_DISTANCE_DP = 70;
    private static final float ANIMATION_IN_ACCELERATION_FACTOR = 0.88f;
    private static final int HOLD_ON_TIME_BEFORE_RELEASING = 350;

    private final PlayerSwipeToSkipExperiment experiment;
    private final PlayerPagerOnboardingStorage storage;
    private final EventBus eventBus;
    private final Handler handler;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    PlayerPagerOnboardingPresenter(PlayerSwipeToSkipExperiment experiment,
                                   PlayerPagerOnboardingStorage storage,
                                   EventBus eventBus) {
        this.experiment = experiment;
        this.storage = storage;
        this.eventBus = eventBus;
        this.handler = new Handler();
    }

    @Override
    public void onResume(PlayerFragment fragment) {
        if (experiment.isEnabled() && !hasReachedMaxOnboardingRun()) {
            final PlayerTrackPager pager = fragment.getPlayerPager();

            subscription = eventBus.subscribe(EventQueue.PLAYER_UI, new ShowOnboardingSubscriber(pager));
        }
    }

    @Override
    public void onPause(PlayerFragment fragment) {
        subscription.unsubscribe();
        handler.removeCallbacksAndMessages(null);
    }

    private boolean hasReachedMaxOnboardingRun() {
        return storage.numberOfOnboardingRun() >= MAX_ONBOARDING_RUN;
    }

    private void showOnboarding(PlayerTrackPager pager) {
        if (!pager.isFakeDragging() && pager.beginFakeDrag()) {
            startAnimation(pager);
        } else {
            Log.e(TAG, "Fake dragging failed to start.");
        }
    }

    private void startAnimation(PlayerTrackPager pager) {
        final int nudgeDistancePx = dpToPx(pager.getContext(), ANIMATION_DISTANCE_DP);
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, nudgeDistancePx);
        valueAnimator.setInterpolator(new AccelerateInterpolator(ANIMATION_IN_ACCELERATION_FACTOR));
        valueAnimator.setDuration(ANIMATION_IN_DURATION_MS);
        valueAnimator.addUpdateListener(new DragPagerListener(pager));
        valueAnimator.addListener(new AnimationListener(pager, HOLD_ON_TIME_BEFORE_RELEASING, handler));
        valueAnimator.start();
    }

    void onOnboardingShown() {
        storage.increaseNumberOfOnboardingRun();
        if (hasReachedMaxOnboardingRun()) {
            subscription.unsubscribe();
        }
    }

    class ShowOnboardingSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        private final PlayerTrackPager playerPager;

        ShowOnboardingSubscriber(PlayerTrackPager playerPager) {
            this.playerPager = playerPager;
        }

        @Override
        public void onNext(PlayerUIEvent event) {
            if (isExpanded(event) && hasNextPage(playerPager)) {
                showOnboarding(playerPager);
                onOnboardingShown();
            }
        }

        private boolean isExpanded(PlayerUIEvent event) {
            return event.getKind() == PlayerUIEvent.PLAYER_EXPANDED;
        }

        private boolean hasNextPage(PlayerTrackPager pager) {
            return pager.getChildCount() > 1;
        }
    }

    static class DragPagerListener implements ValueAnimator.AnimatorUpdateListener {
        private final PlayerTrackPager pager;
        private float lastPositionPx;

        private DragPagerListener(PlayerTrackPager pager) {
            this.pager = pager;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            final float currentValue = (float) valueAnimator.getAnimatedValue();
            fakeDragTo(pager, -currentValue);
        }

        private void fakeDragTo(PlayerTrackPager pager, float nextPositionPx) {
            final float distance = nextPositionPx - lastPositionPx;
            pager.fakeDragBy(distance);
            lastPositionPx = nextPositionPx;
        }
    }

    static class AnimationListener implements Animator.AnimatorListener {
        private final PlayerTrackPager pager;
        private final int timeBeforeDeceleration;
        private final Handler handler;

        private AnimationListener(PlayerTrackPager pager, int timeBeforeDeceleration, Handler handler) {
            this.pager = pager;
            this.timeBeforeDeceleration = timeBeforeDeceleration;
            this.handler = handler;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            handler.postDelayed(() -> pager.endFakeDrag(), timeBeforeDeceleration);
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            pager.endFakeDrag();
        }

        @Override
        public void onAnimationStart(Animator animator) {
            // no-op
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            // no-op
        }
    }
}

