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
import android.view.animation.AccelerateInterpolator;

import javax.inject.Inject;

public class PullToRefreshController {

    private PullToRefreshLayout pullToRefreshLayout;

    @Inject
    public PullToRefreshController() {}

    public void attach(FragmentActivity activity, PullToRefreshLayout pullToRefreshLayout, OnRefreshListener listener) {
        this.pullToRefreshLayout = pullToRefreshLayout;
        ActionBarPullToRefresh.from(activity)
                .allChildrenArePullable()
                .useViewDelegate(EmptyListView.class, new EmptyListDelegate())
                .listener(listener)
                .setup(pullToRefreshLayout);
        styleProgressBar(activity);
    }

    private void styleProgressBar(FragmentActivity activity) {
        SmoothProgressBar spb = (SmoothProgressBar) pullToRefreshLayout.getHeaderView().findViewById(R.id.ptr_progress);
        spb.setIndeterminateDrawable(buildCustomProgressDrawable(activity));
    }

    private SmoothProgressDrawable buildCustomProgressDrawable(Context context) {
       return new SmoothProgressDrawable.Builder(context)
                .interpolator(new AccelerateInterpolator())
                .separatorLength(context.getResources().getDimensionPixelSize(R.dimen.ptr_thickness))
                .strokeWidth(context.getResources().getDimensionPixelSize(R.dimen.ptr_thickness))
                .speed(.8f)
                .color(context.getResources().getColor(R.color.ptr_orange))
                .build();
    }

    public boolean isAttached() {
        return pullToRefreshLayout != null;
    }

    public boolean isRefreshing() {
        return pullToRefreshLayout.isRefreshing();
    }

    public void startRefreshing() {
        pullToRefreshLayout.setRefreshing(true);
    }

    public void stopRefreshing() {
        pullToRefreshLayout.setRefreshComplete();
    }

}
