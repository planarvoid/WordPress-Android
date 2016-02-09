package com.soundcloud.android.view;

import com.soundcloud.android.utils.ViewUtils;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import javax.inject.Inject;

public class NewItemsIndicatorScrollListener extends RecyclerView.OnScrollListener {

    private static final int THRESHOLD_DP = 80;

    private Listener listener;
    private int distance;
    private int threshold;
    private boolean visible = true;
    private boolean autoReset = true;

    @Inject
    NewItemsIndicatorScrollListener(Context context) {
        this.threshold = ViewUtils.dpToPx(context, THRESHOLD_DP);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void disableAutoReset() {
        this.autoReset = false;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if (listener != null) {
            updateDistance(dy);
            setVisibility();
        }
    }

    public void resetVisibility(boolean visible) {
        this.visible = visible;
        this.distance = 0;
    }

    public void destroy() {
        this.listener = null;
        resetVisibility(false);
    }

    private void setVisibility() {
        if (visible && distance > threshold) {
            listener.onScrollHideOverlay();
            if (autoReset) {
                resetVisibility(false);
            }
        } else if (!visible && distance < -threshold) {
            listener.onScrollShowOverlay();
            if (autoReset) {
                resetVisibility(true);
            }
        }
    }

    private void updateDistance(int dy) {
        if ((visible && dy > 0) || (!visible && dy < 0)) {
            distance += dy;
        } else {
            distance = 0;
        }
    }

    public interface Listener {
        void onScrollShowOverlay();

        void onScrollHideOverlay();
    }

}
