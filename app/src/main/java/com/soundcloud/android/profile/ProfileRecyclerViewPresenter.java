package com.soundcloud.android.profile;

import com.soundcloud.android.image.RecyclerViewPauseOnScrollListener;
import com.soundcloud.android.presentation.DividerItemDecoration;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AbsListView;

public abstract class ProfileRecyclerViewPresenter<ItemT> extends RecyclerViewPresenter<ItemT> implements ScrollableProfileItem, RefreshAware {

    private Listener scrollListener;
    private MultiSwipeRefreshLayout pendingRefreshLayout;
    public int scrollState;
    private boolean isResumed;

    protected ProfileRecyclerViewPresenter(PullToRefreshWrapper pullToRefreshWrapper, RecyclerViewPauseOnScrollListener recyclerViewPauseOnScrollListener, DividerItemDecoration dividerItemDecoration) {
        super(pullToRefreshWrapper, recyclerViewPauseOnScrollListener, dividerItemDecoration);
    }

    public void setScrollListener(Listener scrollListener) {
        this.scrollListener = scrollListener;
    }

    public void configureOffsets(int currentHeight, int maxHeight) {
        getEmptyView().setPadding(0, currentHeight, 0, 0);
        final LinearLayoutManager linearLayoutManager = getLinearLayoutManager();
        if (linearLayoutManager.findFirstVisibleItemPosition() == 0
                && scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE){
            linearLayoutManager.scrollToPositionWithOffset(0, currentHeight - maxHeight);
        }
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        getRecyclerView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                ProfileRecyclerViewPresenter.this.scrollState = newState;
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (scrollState == RecyclerView.SCROLL_STATE_DRAGGING || scrollState == RecyclerView.SCROLL_STATE_SETTLING){
                    scrollListener.onVerticalScroll(dy, getVisibleHeaderHeight());
                }
            }
        });
    }

    private int getVisibleHeaderHeight() {
        if (getLinearLayoutManager().getPosition(getRecyclerView().getChildAt(0)) > 0){
            return 0;
        } else {
            return getRecyclerView().getChildAt(0).getTop();
        }
    }

    @Override
    public void onResume(Fragment fragment) {
        super.onResume(fragment);
        isResumed = true;
        if (pendingRefreshLayout != null){
            attachExternalRefreshLayout(pendingRefreshLayout);
            pendingRefreshLayout = null;
        }
    }

    @Override
    public void onPause(Fragment fragment) {
        isResumed = false;
        super.onPause(fragment);
    }

    @Override
    public void attachRefreshLayout(final MultiSwipeRefreshLayout refreshLayout){
        if (isResumed) {
            attachExternalRefreshLayout(refreshLayout);
        } else {
            pendingRefreshLayout = refreshLayout;
        }
    }

    @Override
    public void detachRefreshLayout(){
        detachRefreshWrapper();
        pendingRefreshLayout = null;
    }
}
