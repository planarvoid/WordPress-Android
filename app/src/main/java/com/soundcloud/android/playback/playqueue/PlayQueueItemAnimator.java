package com.soundcloud.android.playback.playqueue;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

class PlayQueueItemAnimator extends RecyclerView.ItemAnimator {

    private static final int ANIMATION_DURATION = 300;

    private List<ChangeHolder> changeList = new ArrayList<>();

    @Override
    public boolean animateDisappearance(@NonNull RecyclerView.ViewHolder viewHolder,
                                        @NonNull ItemHolderInfo preLayoutInfo,
                                        @Nullable ItemHolderInfo postLayoutInfo) {
        dispatchAnimationFinished(viewHolder);
        return false;
    }

    @Override
    public boolean animateAppearance(@NonNull RecyclerView.ViewHolder viewHolder,
                                     @Nullable ItemHolderInfo preLayoutInfo,
                                     @NonNull ItemHolderInfo postLayoutInfo) {
        dispatchAnimationFinished(viewHolder);
        return false;
    }

    @Override
    public boolean animatePersistence(@NonNull RecyclerView.ViewHolder viewHolder,
                                      @NonNull ItemHolderInfo preLayoutInfo,
                                      @NonNull ItemHolderInfo postLayoutInfo) {
        dispatchAnimationFinished(viewHolder);
        return false;
    }

    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
                                 @NonNull RecyclerView.ViewHolder newHolder,
                                 @NonNull ItemHolderInfo preLayoutInfo,
                                 @NonNull ItemHolderInfo postLayoutInfo) {
        boolean isDifferentHolder = oldHolder != newHolder;
        dispatchAnimationFinished(oldHolder);

        if (isDifferentHolder) {
            changeList.add(new ChangeHolder(newHolder, newHolder.itemView.getAlpha()));
            newHolder.itemView.setAlpha(oldHolder.itemView.getAlpha());
        }

        return isDifferentHolder;
    }

    @NonNull
    @Override
    public ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state,
                                                     @NonNull RecyclerView.ViewHolder viewHolder,
                                                     int changeFlags,
                                                     @NonNull List<Object> payloads) {
        return super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads);
    }

    @Override
    public void runPendingAnimations() {
        if (!changeList.isEmpty()) {
            for (final ChangeHolder changeHolder : changeList) {
                startAlphaAnimation(changeHolder.viewHolder, changeHolder.newAlpha);
            }
            changeList.clear();
        } else {
            dispatchAnimationsFinished();
        }
    }

    private void startAlphaAnimation(RecyclerView.ViewHolder viewHolder, float newAlpha) {
        View target = viewHolder.itemView;
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, "alpha", target.getAlpha(), newAlpha);

        animator.setDuration(ANIMATION_DURATION);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addListener(new AnimatorListener(viewHolder));
        animator.start();
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder viewHolder) {
    }

    @Override
    public void endAnimations() {
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        return false;
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder,
                                             @NonNull List<Object> payloads) {
        return false;
    }

    private class AnimatorListener implements Animator.AnimatorListener {
        private final RecyclerView.ViewHolder viewHolder;

        AnimatorListener(RecyclerView.ViewHolder viewHolder) {
            this.viewHolder = viewHolder;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            dispatchAnimationStarted(viewHolder);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            dispatchAnimationFinished(viewHolder);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }

    private class ChangeHolder {

        ChangeHolder(RecyclerView.ViewHolder viewHolder, float newAlpha) {
            this.viewHolder = viewHolder;
            this.newAlpha = newAlpha;
        }

        RecyclerView.ViewHolder viewHolder;
        float newAlpha;
    }
}
