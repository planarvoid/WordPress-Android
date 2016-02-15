package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.annotations.VisibleForTesting;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import javax.inject.Inject;

public class NewItemsIndicator implements NewItemsIndicatorScrollListener.Listener {

    private static final int ANIMATION_DURATION_MS = 500;

    private final NewItemsIndicatorScrollListener scrollListener;
    private final Context context;

    private int newItems;
    private View overlayView;
    @PluralsRes private int textResourceId = Consts.NOT_SET;
    @Nullable private Listener clickListener;

    @Inject
    public NewItemsIndicator(Context context,
                             NewItemsIndicatorScrollListener scrollListener) {
        this.context = context;
        this.scrollListener = scrollListener;

        // We want to control scroll reset to avoid changing state while animating
        scrollListener.disableAutoReset();
    }

    public void setClickListener(Listener listener) {
        this.clickListener = listener;
    }

    public void setTextResourceId(int textResourceId) {
        this.textResourceId = textResourceId;
    }

    @VisibleForTesting
    public int getNewItems() {
        return newItems;
    }

    public void setNewItems(int newItems) {
        this.newItems = newItems;
    }

    public NewItemsIndicatorScrollListener getScrollListener() {
        return scrollListener;
    }

    public void setView(@NonNull View view) {
        this.overlayView = view;
        assignOverlayClickListener();
        scrollListener.setListener(this);

        if (newItems > 0) {
            show();
        }
    }

    public void hideAndReset() {
        hide();
        newItems = 0;
    }

    public void destroy() {
        scrollListener.destroy();
        overlayView = null;
    }

    public void update(int newItems) {
        boolean hasNewItems = this.newItems < newItems;
        this.newItems = newItems;

        if (hasNewItems) {
            show();
        }
    }

    private void hide() {
        scrollListener.resetVisibility(false);

        if (overlayView != null) {
            overlayView.setAnimation(slideOutAnimation());
            overlayView.setVisibility(View.GONE);
        }
    }

    private void show() {
        scrollListener.resetVisibility(true);

        if (overlayView != null && newItems > 0) {
            setOverlayText();
            if (!overlayView.isShown()) {
                overlayView.setAnimation(slideInAnimation());
                overlayView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void assignOverlayClickListener() {
        overlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newItems = 0;
                hide();
                if (clickListener != null) {
                    clickListener.onRefreshableOverlayClicked();
                }
            }
        });
    }

    private void setOverlayText() {
        TextView textView = (TextView) overlayView.findViewById(R.id.refresh_overlay_text);

        if (textView != null && textResourceId != Consts.NOT_SET) {
            String newItemsString = newItems > 9 ? "9+" : String.valueOf(newItems);
            textView.setText(context.getResources().getQuantityString(textResourceId, newItems, newItemsString));
        }
    }

    private Animation slideInAnimation() {
        final Animation animation = AnimationUtils.loadAnimation(context, R.anim.abc_slide_in_top);
        animation.setDuration(ANIMATION_DURATION_MS);
        animation.setInterpolator(new DecelerateInterpolator(2.0f));
        return animation;
    }

    private Animation slideOutAnimation() {
        final Animation animation = AnimationUtils.loadAnimation(context, R.anim.abc_slide_out_top);
        animation.setDuration(ANIMATION_DURATION_MS);
        animation.setInterpolator(new AccelerateInterpolator(2.0f));
        return animation;
    }

    private boolean isAnimating() {
        Animation animation = overlayView.getAnimation();
        return animation != null && (animation.hasStarted() && !animation.hasEnded());
    }

    @Override
    public void onScrollHideOverlay() {
        if (overlayView != null && overlayView.isShown() && !isAnimating()) {
            hide();
        }
    }

    @Override
    public void onScrollShowOverlay() {
        if (overlayView != null && !overlayView.isShown() && !isAnimating()) {
            show();
        }
    }

    public interface Listener {
        void onRefreshableOverlayClicked();
    }

}
