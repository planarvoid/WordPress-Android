package com.soundcloud.android.profile;

import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

abstract class ProfileRecyclerViewPresenter<ItemT> extends RecyclerViewPresenter<ItemT> implements RefreshableProfileItem {

    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final ProfileRecyclerViewScroller scroller;


    private MultiSwipeRefreshLayout pendingRefreshLayout;
    private boolean isResumed;

    protected ProfileRecyclerViewPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                           ImagePauseOnScrollListener imagePauseOnScrollListener,
                                           ProfileRecyclerViewScroller profileRecyclerViewScroller) {
        super(swipeRefreshAttacher);
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        scroller = profileRecyclerViewScroller;
    }

    public ScrollableProfileItem getScrollableItem() {
        return scroller;
    }

    @Override
    public void onViewCreated(Fragment fragment, final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        scroller.setViews(getRecyclerView(), getEmptyView());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        scroller.clearViews();
        super.onDestroyView(fragment);
    }

    @Override
    public void onResume(Fragment fragment) {
        super.onResume(fragment);

        isResumed = true;
        if (pendingRefreshLayout != null) {
            attachSwipeToRefresh(pendingRefreshLayout, getRecyclerView(), getEmptyView());
            pendingRefreshLayout = null;
        }
    }

    @Override
    public void onPause(Fragment fragment) {
        isResumed = false;
        super.onPause(fragment);
    }

    @Override
    public void attachRefreshLayout(MultiSwipeRefreshLayout refreshLayout){
        if (isResumed) {
            attachSwipeToRefresh(refreshLayout, getRecyclerView(), getEmptyView());
        } else {
            pendingRefreshLayout = refreshLayout;
        }
    }

    @Override
    public void detachRefreshLayout(){
        detachSwipeToRefresh();
        pendingRefreshLayout = null;
    }
}
