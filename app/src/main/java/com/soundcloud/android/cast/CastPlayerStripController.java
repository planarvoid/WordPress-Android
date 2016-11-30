package com.soundcloud.android.cast;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.ui.view.PlayerStripView;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.DefaultAnimationListener;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;

@AutoFactory(allowSubclasses = true)
public class CastPlayerStripController {

    private static final long ANIMATION_DURATION = 300;
    private final ValueAnimator collapseAnimator;
    private boolean isPlayerExpanded = false;
    private Subscription subscription;

    private final PlayerStripView castView;
    private final CastConnectionHelper castConnectionHelper;
    private final EventBus eventBus;
    private final ValueAnimator expandAnimator;


    CastPlayerStripController(PlayerStripView view,
                              @Provided CastConnectionHelper castConnectionHelper,
                              @Provided EventBus eventBus) {
        this.castView = view;
        this.castConnectionHelper = castConnectionHelper;
        this.eventBus = eventBus;

        expandAnimator = createExpandAnimator();
        collapseAnimator = createCollapseAnimator();

        subscribeToEvents();
    }

    public void updateWithoutAnimation() {
        if (isPlayerExpanded && castConnectionHelper.isCasting()) {
            setExpanded();
        } else {
            setCollapsed();
        }
    }

    public void setCollapsed() {
        castView.getLayoutParams().height = castView.getCollapsedHeight();
        castView.requestLayout();
    }

    private void setExpanded() {
        castView.getLayoutParams().height = castView.getExpandedHeight();
        castView.requestLayout();

        castView.getCastDeviceName().setText(castConnectionHelper.getDeviceName());
    }

    private void subscribeToEvents() {
        subscription = eventBus.queue(EventQueue.PLAYER_UI).subscribe(new PlayerStateSubscriber());
    }

    public void clear() {
        subscription.unsubscribe();
    }

    public void update() {
        if (isPlayerExpanded && castConnectionHelper.isCasting()) {
            expand();
        } else {
            collapse();
        }
    }

    private void expand() {
        if (!castView.isExpanded()) {
            expandAnimator.start();
        }
    }

    private void collapse() {
        if (!castView.isCollapsed()) {
            collapseAnimator.start();
        }
    }

    public void setHeightFromCollapse(float slideAnimateValue) {
        if (castConnectionHelper.isCasting()) {
            castView.getLayoutParams().height = calculateNewHeight(slideAnimateValue);
            castView.requestLayout();

            castView.getCastDeviceName().setAlpha(slideAnimateValue);
            castView.getCastDeviceName().setText(castConnectionHelper.getDeviceName());
        }
    }

    private int calculateNewHeight(float slideAnimateValue) {
        int heightDiff = castView.getExpandedHeight() - castView.getCollapsedHeight();
        return castView.getExpandedHeight() - (int) ((1 - slideAnimateValue) * heightDiff);
    }

    private ValueAnimator createCollapseAnimator() {
        return createValueAnimator(castView.getExpandedHeight(), castView.getCollapsedHeight());
    }

    private ValueAnimator createExpandAnimator() {
        final ValueAnimator animator = createValueAnimator(castView.getCollapsedHeight(), castView.getExpandedHeight());
        animator.addListener(new DefaultAnimationListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                castView.getCastDeviceName().setText(castConnectionHelper.getDeviceName());
            }
        });
        return animator;
    }

    private ValueAnimator createValueAnimator(int fromValue, int toValue) {
        final ValueAnimator animator = ObjectAnimator.ofInt(fromValue, toValue);
        animator.addUpdateListener(new RedrawLayoutListener(castView));
        animator.setDuration(ANIMATION_DURATION);
        return animator;
    }

    private class PlayerStateSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            isPlayerExpanded = event.getKind() == PlayerUIEvent.PLAYER_EXPANDED;
        }
    }

    private static class RedrawLayoutListener implements ValueAnimator.AnimatorUpdateListener {

        private final View layout;

        RedrawLayoutListener(View layout) {
            this.layout = layout;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            layout.getLayoutParams().height = (int) valueAnimator.getAnimatedValue();
            layout.requestLayout();
        }
    }
}
