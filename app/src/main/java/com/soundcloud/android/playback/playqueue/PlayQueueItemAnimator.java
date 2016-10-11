package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.R;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.animation.AnimatorCompatHelper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class PlayQueueItemAnimator extends RecyclerView.ItemAnimator {

    enum Mode {
        DEFAULT, SHUFFLING, REPEAT
    }

    private static final int ALPHA_ANIMATION_DURATION = 300;
    private static final int MOVE_ANIMATION_DURATION = 500;
    private static final int MOVE_FADE_OUT_ANIMATION_DURATION = 300;
    private static final int MOVE_FADE_IN_ANIMATION_DURATION = 200;

    private final ArrayList<RecyclerView.ViewHolder> pendingMoves = new ArrayList<>();
    private final ArrayList<ArrayList<RecyclerView.ViewHolder>> movesList = new ArrayList<>();
    private final ArrayList<RecyclerView.ViewHolder> moveAnimations = new ArrayList<>();
    private final List<ChangeHolder> alphaAnimations = new ArrayList<>();

    private final Random random;
    private Mode mode;

    private class ChangeHolder {

        final RecyclerView.ViewHolder viewHolder;
        final float newAlpha;

        ChangeHolder(RecyclerView.ViewHolder viewHolder, float newAlpha) {
            this.viewHolder = viewHolder;
            this.newAlpha = newAlpha;
        }
    }

    @Inject
    public PlayQueueItemAnimator() {
        this.random = new Random();
        this.mode = Mode.DEFAULT;
    }

    public void setMode(Mode newMode) {
        mode = newMode;
        endAnimations();
    }

    @Override
    public void runPendingAnimations() {

        final boolean hadPendingMoves = pendingMoves.isEmpty();
        final boolean hasPendingAlphaAnimation = alphaAnimations.isEmpty();

        if (!hadPendingMoves && !hasPendingAlphaAnimation) {
            dispatchFinishedWhenDone();
        } else {
            runPendingAnimationsImpl(hadPendingMoves, hasPendingAlphaAnimation);
        }
    }

    private void runPendingAnimationsImpl(boolean hadPendingMoves, boolean hasPendingAlphaAnimation) {
        if (!hadPendingMoves) {
            final ArrayList<RecyclerView.ViewHolder> moves = newArrayList(pendingMoves);
            pendingMoves.clear();

            movesList.add(moves);
            for (RecyclerView.ViewHolder holder : moves) {
                animateMoveImpl(holder);
            }
            moves.clear();
            movesList.remove(moves);
        }

        if (!hasPendingAlphaAnimation) {
            for (final ChangeHolder changeHolder : alphaAnimations) {
                startAlphaAnimation(changeHolder.viewHolder, changeHolder.newAlpha);
            }
            alphaAnimations.clear();
        }
    }

    private void startAlphaAnimation(final RecyclerView.ViewHolder viewHolder, float newAlpha) {
        View target = viewHolder.itemView;
        View image = target.findViewById(R.id.image);
        View textHolder = target.findViewById(R.id.text_holder);
        ObjectAnimator imageAnimator = ObjectAnimator.ofFloat(image, "alpha", image.getAlpha(), newAlpha);
        ObjectAnimator textAnimator = ObjectAnimator.ofFloat(textHolder, "alpha", textHolder.getAlpha(), newAlpha);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(imageAnimator, textAnimator);

        animatorSet.setDuration(ALPHA_ANIMATION_DURATION);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                dispatchAnimationStarted(viewHolder);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchFinishedWhenDone();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animatorSet.start();
    }

    @Override
    public boolean animateAppearance(@NonNull RecyclerView.ViewHolder viewHolder,
                                     @Nullable ItemHolderInfo preLayoutInfo, @NonNull ItemHolderInfo postLayoutInfo) {
        final int postTop = postLayoutInfo.top;
        final int preTop;

        if (preLayoutInfo != null) {
            preTop = preLayoutInfo.top;
        } else {
            final int relativePosition = getRandomNumberInInterval(5, 10);
            // It appears from the bottom
            preTop = postTop + relativePosition * viewHolder.itemView.getHeight();
        }
        return performAnimationIfAny(viewHolder, preTop, postTop);
    }

    private int getRandomNumberInInterval(int intervalStart, int intervalStop) {
        return intervalStart + random.nextInt(intervalStop);
    }

    @Override
    public boolean animatePersistence(@NonNull RecyclerView.ViewHolder viewHolder,
                                      @NonNull ItemHolderInfo preLayoutInfo,
                                      @NonNull ItemHolderInfo postLayoutInfo) {
        return performAnimationIfAny(viewHolder, preLayoutInfo.top, postLayoutInfo.top);
    }

    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
                                 @NonNull RecyclerView.ViewHolder newHolder,
                                 @NonNull ItemHolderInfo preLayoutInfo,
                                 @NonNull ItemHolderInfo postLayoutInfo) {
        if (oldHolder != newHolder) {
            dispatchAnimationFinished(oldHolder);
        }

        if (mode == Mode.SHUFFLING) {
            performAnimationIfAny(newHolder, preLayoutInfo.top, postLayoutInfo.top);
            return true;
        } else if (mode == Mode.REPEAT) {
            BackgroundInfoHolder backgroundInfoHolder = (BackgroundInfoHolder) postLayoutInfo;
            BackgroundInfoHolder prebackgroundInfoHolder = (BackgroundInfoHolder) preLayoutInfo;
            float preAlpha = prebackgroundInfoHolder.alpha;
            float postAlpha = backgroundInfoHolder.alpha;
            if (preAlpha != postAlpha) {
                return performRepeatAnimation(newHolder, preAlpha, postAlpha);
            } else {
                return false;
            }
        } else {
            dispatchAnimationFinished(newHolder);
            return false;
        }
    }

    private boolean performRepeatAnimation(@NonNull RecyclerView.ViewHolder newHolder,
                                           float preAlpha,
                                           float postAlpha) {
        switch (newHolder.itemView.getId()) {
            case R.id.play_queue_item_header:
                performHeaderRepeatAnimation(newHolder, preAlpha, postAlpha);
                return true;
            case R.id.play_queue_item_track:
                performTrackRepeatAnimation(newHolder, preAlpha, postAlpha);
                return true;
            default:
                throw new IllegalStateException("Unknown item type.");
        }
    }

    private void performHeaderRepeatAnimation(@NonNull RecyclerView.ViewHolder newHolder,
                                              float preAlpha,
                                              float postAlpha) {
        final View view = newHolder.itemView;
        final View textView = view.findViewById(R.id.title);
        ViewCompat.setAlpha(textView, preAlpha);
        alphaAnimations.add(new ChangeHolder(newHolder, postAlpha));
        textView.setAlpha(preAlpha);
    }

    private void performTrackRepeatAnimation(@NonNull RecyclerView.ViewHolder newHolder,
                                             float preAlpha,
                                             float postAlpha) {
        final View view = newHolder.itemView;
        final View imageView = view.findViewById(R.id.image);
        final View textView = view.findViewById(R.id.text_holder);
        ViewCompat.setAlpha(imageView, preAlpha);
        ViewCompat.setAlpha(textView, preAlpha);
        alphaAnimations.add(new ChangeHolder(newHolder, postAlpha));
        imageView.setAlpha(preAlpha);
        textView.setAlpha(preAlpha);
    }

    @Override
    public boolean animateDisappearance(@NonNull RecyclerView.ViewHolder viewHolder,
                                        @NonNull ItemHolderInfo preLayoutInfo,
                                        @Nullable ItemHolderInfo postLayoutInfo) {

        final int preTop = preLayoutInfo.top;
        final int postTop;

        if (postLayoutInfo != null) {
            postTop = postLayoutInfo.top;
        } else {
            final View disappearingItemView = viewHolder.itemView;
            postTop = estimateDisappearanceToY(viewHolder, preTop, disappearingItemView.getHeight());
            disappearingItemView.layout(0,
                                        postTop,
                                        disappearingItemView.getWidth(),
                                        postTop + disappearingItemView.getHeight());
        }
        return performAnimationIfAny(viewHolder, preTop, postTop);
    }

    private boolean performAnimationIfAny(@NonNull RecyclerView.ViewHolder newHolder, int preTop, int postTop) {
        if (mode == Mode.SHUFFLING && isMoving(preTop, postTop)) {
            animateMove(newHolder, preTop, postTop);
            return true;
        } else {
            dispatchAnimationFinished(newHolder);
            return false;
        }
    }

    private boolean isMoving(int preTop, int postTop) {
        return preTop != postTop;
    }

    private int estimateDisappearanceToY(@NonNull RecyclerView.ViewHolder viewHolder, int oldTop, int height) {
        final int newTop;
        if (viewHolder.getAdapterPosition() == -1) {
            // We don't know the new position so use a random position
            newTop = oldTop + getRandomNumberInInterval(7, 12) * height;
        } else {
            final int position = viewHolder.getAdapterPosition();
            // There is an approximation here as the first item in the adapter
            // is not necessary at position 0 on screen.
            newTop = position * height;
        }
        return newTop;
    }

    private void animateMove(final RecyclerView.ViewHolder holder,
                             int fromY, int toY) {
        final View view = holder.itemView;
        fromY += ViewCompat.getTranslationY(holder.itemView);
        resetAnimation(holder);
        int deltaY = toY - fromY;
        if (deltaY != 0) {
            ViewCompat.setTranslationY(view, -deltaY);
        }
        pendingMoves.add(holder);
    }

    private void animateMoveImpl(final RecyclerView.ViewHolder holder) {
        final View view = holder.itemView;

        moveAnimations.add(holder);

        final AnimatorSet animSetXY = new AnimatorSet();

        animSetXY.play(ObjectAnimator.ofFloat(view, "translationY", 0f).setDuration(MOVE_ANIMATION_DURATION));

        final float originalAlpha = view.getAlpha();
        if (originalAlpha == 1) {
            animSetXY.playSequentially(
                    ObjectAnimator.ofFloat(view, "alpha", 0.5f).setDuration(MOVE_FADE_OUT_ANIMATION_DURATION),
                    ObjectAnimator.ofFloat(view, "alpha", 1f).setDuration(MOVE_FADE_IN_ANIMATION_DURATION)
            );
        }

        animSetXY.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animSetXY.removeListener(this);
                moveAnimations.remove(holder);
                dispatchFinishedWhenDone();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                ViewCompat.setTranslationY(view, 0);
                ViewCompat.setAlpha(view, originalAlpha);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animSetXY.start();
    }


    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        final View view = item.itemView;
        // this will trigger end callback which should set properties to their target values.
        ViewCompat.animate(view).cancel();
        // TODO if some other animations are chained to end, how do we cancel them as well?
        for (int i = pendingMoves.size() - 1; i >= 0; i--) {
            final RecyclerView.ViewHolder holder = pendingMoves.get(i);
            if (holder == item) {
                ViewCompat.setTranslationY(view, 0);
                pendingMoves.remove(i);
            }
        }

        for (int i = movesList.size() - 1; i >= 0; i--) {
            ArrayList<RecyclerView.ViewHolder> moves = movesList.get(i);
            for (int j = moves.size() - 1; j >= 0; j--) {
                final RecyclerView.ViewHolder holder = moves.get(j);
                if (holder == item) {
                    ViewCompat.setTranslationY(view, 0);
                    moves.remove(j);
                    if (moves.isEmpty()) {
                        movesList.remove(i);
                    }
                    break;
                }
            }
        }

        dispatchFinishedWhenDone();
    }

    private void resetAnimation(RecyclerView.ViewHolder holder) {
        AnimatorCompatHelper.clearInterpolator(holder.itemView);
        endAnimation(holder);
    }

    @Override
    public boolean isRunning() {
        return (!pendingMoves.isEmpty() ||
                !moveAnimations.isEmpty() ||
                !movesList.isEmpty() ||
                !alphaAnimations.isEmpty());
    }

    /**
     * Check the state of currently pending and running animations. If there are none
     * pending/running, call {@link #dispatchAnimationsFinished()} to notify any
     * listeners.
     */
    private void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    @Override
    public void endAnimations() {
        int count = pendingMoves.size();
        for (int i = count - 1; i >= 0; i--) {
            final RecyclerView.ViewHolder holder = pendingMoves.get(i);
            final View view = holder.itemView;
            ViewCompat.setTranslationY(view, 0);
            pendingMoves.remove(i);
        }

        if (!isRunning()) {
            return;
        }

        int listCount = movesList.size();
        for (int i = listCount - 1; i >= 0; i--) {
            final ArrayList<RecyclerView.ViewHolder> moves = movesList.get(i);
            count = moves.size();
            for (int j = count - 1; j >= 0; j--) {
                final RecyclerView.ViewHolder holder = moves.get(j);
                final View view = holder.itemView;
                ViewCompat.setTranslationY(view, 0);
                moves.remove(j);
                if (moves.isEmpty()) {
                    movesList.remove(moves);
                }
            }
        }
        cancelAll(moveAnimations);

        for (int i = alphaAnimations.size() - 1; i >= 0; i--) {
            final RecyclerView.ViewHolder viewHolder = alphaAnimations.get(i).viewHolder;
            final View view = viewHolder.itemView;

            switch (view.getId()) {
                case R.id.play_queue_item_header:
                    ViewCompat.animate(view.findViewById(R.id.title)).cancel();
                    break;
                case R.id.play_queue_item_track:
                    ViewCompat.animate(view.findViewById(R.id.image)).cancel();
                    ViewCompat.animate(view.findViewById(R.id.text_holder)).cancel();
                    break;
                default:
                    throw new IllegalStateException("Unknown item type.");
            }
        }

        dispatchAnimationsFinished();
    }

    void cancelAll(List<RecyclerView.ViewHolder> viewHolders) {
        for (int i = viewHolders.size() - 1; i >= 0; i--) {
            ViewCompat.animate(viewHolders.get(i).itemView).cancel();
        }
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder,
                                             @NonNull List<Object> payloads) {
        return true;
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        return true;
    }

    @NonNull
    @Override
    public ItemHolderInfo recordPostLayoutInformation(@NonNull RecyclerView.State state,
                                                      @NonNull RecyclerView.ViewHolder viewHolder) {
        if (mode == Mode.SHUFFLING) {
            return super.recordPostLayoutInformation(state, viewHolder);
        } else {
            return BackgroundInfoHolder.create(viewHolder);
        }
    }

    @NonNull
    @Override
    public ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state,
                                                     @NonNull RecyclerView.ViewHolder viewHolder,
                                                     int changeFlags,
                                                     @NonNull List<Object> payloads) {
        if (mode == Mode.SHUFFLING) {
            return super.recordPostLayoutInformation(state, viewHolder);
        } else {
            return BackgroundInfoHolder.create(viewHolder);
        }
    }

    static class BackgroundInfoHolder extends ItemHolderInfo {

        private float alpha;

        private BackgroundInfoHolder(float alpha) {
            this.alpha = alpha;
        }

        static final BackgroundInfoHolder create(RecyclerView.ViewHolder viewHolder) {
            View view;
            switch (viewHolder.itemView.getId()) {
                case R.id.play_queue_item_header:
                    view = viewHolder.itemView;
                    break;
                case R.id.play_queue_item_track:
                    view = viewHolder.itemView.findViewById(R.id.image);
                    break;
                default:
                    throw new IllegalStateException("Unknown item type.");
            }
            return new BackgroundInfoHolder(view.getAlpha());
        }

    }
}
