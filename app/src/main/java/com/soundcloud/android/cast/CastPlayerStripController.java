package com.soundcloud.android.cast;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.ui.view.PlayerStripView;
import com.soundcloud.android.playback.ui.view.PlayerUpsellView;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.DefaultAnimationListener;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@AutoFactory(allowSubclasses = true)
public class CastPlayerStripController {

    private static final long ANIMATION_DURATION = 300;

    private boolean isPlayerExpanded = false;
    private Subscription subscription;

    private final PlayerStripView stripView;
    private final PlayerUpsellView upsellView;
    private final CastConnectionHelper castConnectionHelper;
    private final EventBus eventBus;

    private final AnimatorSet expandCastAndCollapseUpsellAnimators;
    private final AnimatorSet collapseCastAndExpandUpsellAnimators;
    private final ValueAnimator collapseCastBarAnimator;
    private final ValueAnimator expandCastBarAnimator;

    CastPlayerStripController(PlayerStripView playerStripView,
                              PlayerUpsellView playerUpsellView,
                              @Provided CastConnectionHelper castConnectionHelper,
                              @Provided EventBus eventBus) {
        this.stripView = playerStripView;
        this.upsellView = playerUpsellView;
        this.castConnectionHelper = castConnectionHelper;
        this.eventBus = eventBus;

        collapseCastBarAnimator = stripView.createCollapseAnimator().setDuration(ANIMATION_DURATION);
        collapseCastAndExpandUpsellAnimators = createAnimatorSet(upsellView.getExpandAnimators(), collapseCastBarAnimator);

        expandCastBarAnimator = getExpandAnimator();
        expandCastAndCollapseUpsellAnimators = createAnimatorSet(upsellView.getCollapseAnimators(), expandCastBarAnimator);

        subscribeToEvents();
    }

    private ValueAnimator getExpandAnimator() {
        return stripView.createExpandAnimator(new DefaultAnimationListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                stripView.updateCastDeviceName(getCastDeviceName());
            }
        }).setDuration(ANIMATION_DURATION);
    }

    private AnimatorSet createAnimatorSet(List<ValueAnimator> upsellAnimators, ValueAnimator castBarAnimator) {
        final Collection<Animator> animators = new ArrayList<>(4);
        animators.addAll(upsellAnimators);
        animators.add(castBarAnimator);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators);
        animatorSet.setDuration(ANIMATION_DURATION);
        return animatorSet;
    }

    public void updateWithoutAnimation() {
        if (isPlayerExpanded && castConnectionHelper.isCasting()) {
            expand(false);
        } else {
            collapse(false);
        }
    }

    private void subscribeToEvents() {
        subscription = eventBus.queue(EventQueue.PLAYER_UI).subscribe(new PlayerStateSubscriber());
    }

    public void clear() {
        subscription.unsubscribe();
    }

    public void update(boolean animate) {
        if (isPlayerExpanded && castConnectionHelper.isCasting()) {
            expand(animate);
        } else {
            collapse(animate);
        }
    }

    private void expand(boolean animate) {
        if (animate && !stripView.isExpanded()) {
            animateExpand();
        }

        stripView.setExpanded();
        stripView.updateCastDeviceName(getCastDeviceName());
        if (upsellView.isVisible()) {
            upsellView.setCollapsed();
        }
    }

    private void animateExpand() {
        if (upsellView.isVisible()) {
            expandCastAndCollapseUpsellAnimators.start();
        } else {
            expandCastBarAnimator.start();
        }
    }

    private void collapse(boolean animate) {
        if (animate && !stripView.isCollapsed()) {
            animateCollapse();
        }

        stripView.setCollapsed();
        if (upsellView.isVisible()) {
            upsellView.setExpanded();
        }
    }

    private void animateCollapse() {
        if (upsellView.isVisible()) {
            collapseCastAndExpandUpsellAnimators.start();
        } else {
            collapseCastBarAnimator.start();
        }
    }

    public void setHeightFromCollapse(float slideAnimateValue) {
        if (castConnectionHelper.isCasting()) {
            stripView.setHeight(calculateNewHeight(slideAnimateValue));
            stripView.updateCastDeviceName(getCastDeviceName(), slideAnimateValue);
        }
    }

    private int calculateNewHeight(float slideAnimateValue) {
        int heightDiff = stripView.getExpandedHeight() - stripView.getCollapsedHeight();
        return stripView.getExpandedHeight() - (int) ((1 - slideAnimateValue) * heightDiff);
    }

    private String getCastDeviceName() {
        return stripView.getResources().getString(R.string.cast_casting_to_device, castConnectionHelper.getDeviceName());
    }

    private class PlayerStateSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            isPlayerExpanded = event.getKind() == PlayerUIEvent.PLAYER_EXPANDED;
        }
    }

}
