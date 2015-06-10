package com.soundcloud.android.profile;

import com.soundcloud.android.view.EmptyView;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class ProfileRecyclerViewScroller extends ProfileEmptyViewScroller {

    @Nullable private RecyclerView recyclerView;

    private int scrollState;
    private LinearLayoutManager linearLayoutManager;
    private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            scrollState = newState;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (scrollState == RecyclerView.SCROLL_STATE_DRAGGING || scrollState == RecyclerView.SCROLL_STATE_SETTLING) {
                getScrollListener().onVerticalScroll(dy, getVisibleHeaderHeight());
            }
        }
    };

    @Inject
    public ProfileRecyclerViewScroller(Resources resources) {
        super(resources);
    }

    public void setViews(RecyclerView recyclerView, EmptyView emptyView){
        super.setView(emptyView);
        this.recyclerView = recyclerView;
        linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(onScrollListener);
    }

    public void clearViews(){
        super.clearViews();
        if (recyclerView != null){
            recyclerView.removeOnScrollListener(onScrollListener);
        }
    }

    @Override
    protected void configureTopEdges(int currentHeight) {
        super.configureTopEdges(currentHeight);

        if (recyclerView != null) {
            final View firstChild = linearLayoutManager.getChildAt(0);

            // do not user findFirstVisibleItemPosition as it will not work with the padding
            if ((firstChild == null || linearLayoutManager.getPosition(firstChild) == 0)
                    && scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                final int offset = currentHeight - getMaximumHeight();
                linearLayoutManager.scrollToPositionWithOffset(0, offset);
            }
        }
    }

    private int getVisibleHeaderHeight() {
        if (recyclerView == null || linearLayoutManager.getPosition(recyclerView.getChildAt(0)) > 0) {
            return 0;
        } else {
            return recyclerView.getChildAt(0).getTop();
        }
    }

}
