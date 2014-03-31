package com.soundcloud.android.actionbar;

import com.soundcloud.android.R;
import com.soundcloud.android.view.EmptyListDelegate;
import com.soundcloud.android.view.EmptyListView;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.animation.DecelerateInterpolator;

import javax.inject.Inject;

public class PullToRefreshController {

    private PullToRefreshLayout mPullToRefreshLayout;

    @Inject
    public PullToRefreshController() {}

    public void attach(FragmentActivity activity, PullToRefreshLayout pullToRefreshLayout, OnRefreshListener listener) {
        mPullToRefreshLayout = pullToRefreshLayout;
        ActionBarPullToRefresh.from(activity)
                .allChildrenArePullable()
                .useViewDelegate(EmptyListView.class, new EmptyListDelegate())
                .listener(listener)
                .setup(mPullToRefreshLayout);
        styleProgressBar(activity);
    }

    private void styleProgressBar(FragmentActivity activity) {
        SmoothProgressBar spb = (SmoothProgressBar) mPullToRefreshLayout.getHeaderView().findViewById(R.id.ptr_progress);
        spb.setIndeterminateDrawable(buildCustomProgressDrawable(activity));
    }

    private SmoothProgressDrawable buildCustomProgressDrawable(Context context) {
       return new SmoothProgressDrawable.Builder(context)
                .interpolator(new DecelerateInterpolator())
                .sectionsCount(3)
                .separatorLength(0)
                .strokeWidth(context.getResources().getDimensionPixelSize(R.dimen.ptr_thickness))
                .speed(1f)
                .reversed(true)
                .mirrorMode(true)
                .colors(context.getResources().getIntArray(R.array.ptr_colors))
                .build();
    }

    public boolean isAttached() {
        return mPullToRefreshLayout != null;
    }

    public boolean isRefreshing() {
        return mPullToRefreshLayout.isRefreshing();
    }

    public void startRefreshing() {
        mPullToRefreshLayout.setRefreshing(true);
    }

    public void stopRefreshing() {
        mPullToRefreshLayout.setRefreshComplete();
    }

}
