package com.soundcloud.android.view;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;

public class ScListView extends PullToRefreshListView implements PullToRefreshBase.OnConfigureHeaderListener {
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

        getRefreshableView().setFadingEdgeLength((int) (2 * res.getDisplayMetrics().density));
        getRefreshableView().setSelector(R.drawable.list_selector_background);
        getRefreshableView().setLongClickable(false);
        getRefreshableView().setScrollingCacheEnabled(false);
        getRefreshableView().setCacheColorHint(Color.WHITE);
        getLoadingLayoutProxy().setRefreshingLabel(getContext().getString(R.string.updating));
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
    public void onConfigureHeader() {
        if (!isRefreshing()) {
            if (mLastUpdated > 0) {
                getLoadingLayoutProxy().setLastUpdatedLabel(getResources().getString(R.string.pull_to_refresh_last_updated,
                        ScTextUtils.getElapsedTimeString(getResources(), mLastUpdated, true)));
            } else {
                getLoadingLayoutProxy().setLastUpdatedLabel("");
            }
        }
    }
}
