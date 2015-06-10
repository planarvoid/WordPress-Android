package com.soundcloud.android.profile;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.view.EmptyView;

import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.Nullable;

class ProfileEmptyViewScroller implements ScrollableProfileItem {

    @Nullable private EmptyView emptyView;

    private ScrollableProfileItem.Listener scrollListener;

    private int maximumHeight;
    private int lastRequestedTopSpace = Consts.NOT_SET;

    private final Handler handler = new Handler();
    private final Runnable configureTopRunnable = new Runnable() {
        @Override
        public void run() {
            configureTopEdges(lastRequestedTopSpace == Consts.NOT_SET ? maximumHeight : lastRequestedTopSpace);
        }
    };

    public ProfileEmptyViewScroller(Resources resources) {
        maximumHeight = resources.getDimensionPixelSize(R.dimen.profile_header_expanded_height);
    }

    public void setView(EmptyView emptyView){
        this.emptyView = emptyView;
        handler.post(configureTopRunnable);
    }

    public void clearViews(){
        emptyView = null;
    }

    public void setScrollListener(ScrollableProfileItem.Listener scrollListener) {
        this.scrollListener = scrollListener;
    }

    public Listener getScrollListener() {
        return scrollListener;
    }

    public void configureOffsets(int topSpace) {
        if (topSpace != lastRequestedTopSpace) {
            removePendingConfigureCalls();
            lastRequestedTopSpace = topSpace;
            configureTopEdges(topSpace);
        }
    }

    public int getMaximumHeight() {
        return maximumHeight;
    }

    private void removePendingConfigureCalls() {
        handler.removeCallbacks(configureTopRunnable);
    }

    protected void configureTopEdges(int currentHeight) {
        if (emptyView != null){
            emptyView.setPadding(0, currentHeight, 0, 0);
        }
    }
}
