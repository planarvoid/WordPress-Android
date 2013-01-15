package com.soundcloud.android.view;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

public class ScListView extends PullToRefreshListView implements PullToRefreshBase.OnConfigureHeaderListener {
    private View mEmptyView;
    private long mLastUpdated;

    public ScListView(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public ScListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        final Resources res = getResources();

        mEmptyView = new EmptyListView(getContext());
        getRefreshableView().setFadingEdgeLength((int) (2 * res.getDisplayMetrics().density));
        getRefreshableView().setSelector(R.drawable.list_selector_background);
        getRefreshableView().setLongClickable(false);
        getRefreshableView().setScrollingCacheEnabled(false);
        getRefreshableView().setCacheColorHint(Color.WHITE);
        setOnConfigureHeaderListener(this);
        setShowIndicator(false); // we don't want the indicator, it interferes with out timestamps
    }

    public void setLastUpdated(long time) {
        mLastUpdated = time;
        onConfigureHeader();
    }

    @Override
    public int getSolidColor() {
        return 0x666666;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            configEmptyViewDimensions();
        }
    }

    private void configEmptyViewDimensions() {
        if (getHeight() > 0 && mEmptyView != null && mEmptyView.findViewById(R.id.sizer) != null) {
        mEmptyView.findViewById(R.id.sizer).setMinimumHeight(getHeight());
        mEmptyView.findViewById(R.id.sizer).requestLayout();
        }
    }

    @Override
    public void onConfigureHeader() {
        if (!isRefreshing()) {
            if (mLastUpdated > 0) {
                setLastUpdatedLabel(getResources().getString(R.string.pull_to_refresh_last_updated,
                        ScTextUtils.getElapsedTimeString(getResources(), mLastUpdated, true)));
            } else {
                setLastUpdatedLabel("");
            }
        }
    }
}
