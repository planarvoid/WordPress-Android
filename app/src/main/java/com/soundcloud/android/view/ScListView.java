package com.soundcloud.android.view;


import com.google.android.imageloader.ImageLoader;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;

/*
pull to refresh from : https://github.com/chrisbanes/Android-PullToRefresh/tree/7e918327cad2d217e909147d82882f50c2e3f59a
 */

public class ScListView extends PullToRefreshListView implements AbsListView.OnScrollListener, PullToRefreshBase.OnFlingListener, ImageLoader.LoadBlocker, PullToRefreshBase.OnConfigureHeaderListener {

    @SuppressWarnings({"UnusedDeclaration"})
    private static final String TAG = "ScListView";

    private View mEmptyView;
    private long mLastUpdated;

    public ScListView(Context context) {
        super(context);
        init();
    }

    /**
     * @noinspection UnusedDeclaration
     */
    public ScListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        final Resources res = getResources();

        mEmptyView = new EmptyCollection(getContext());
        getRefreshableView().setFadingEdgeLength((int) (2 * res.getDisplayMetrics().density));
        getRefreshableView().setSelector(R.drawable.list_selector_background);
        getRefreshableView().setLongClickable(false);
        getRefreshableView().setScrollingCacheEnabled(false);
        getRefreshableView().setBackgroundColor(Color.WHITE);
        getRefreshableView().setCacheColorHint(Color.WHITE);
        setOnFlingListener(this);
        setOnConfigureHeaderListener(this);
        setShowIndicator(false); // we don't want the indicator, it interferes with out timestamps
    }

    public void setLastUpdated(long time) {
        mLastUpdated = time;
        onConfigureHeader();
    }

    /*
     We still have to use a custom view controlled by our adapter. Their solution didn't work at the time of integration.
     */
    public final void setCustomEmptyView(View newEmptyView) {
        mEmptyView = newEmptyView;
        configEmptyViewDimensions();
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

    public View getCustomEmptyView() {
        return mEmptyView;
    }

    @Override
    public void onFling() {
        ImageLoader.get(getContext()).block(this);
    }

    @Override
    public void onFlingDone() {
        ImageLoader.get(getContext()).unblock(this);
    }

    @Override
    public void onConfigureHeader() {
        if (mLastUpdated > 0) {
            setLastUpdatedLabel(getResources().getString(R.string.pull_to_refresh_last_updated,
                    ScTextUtils.getElapsedTimeString(getResources(), mLastUpdated, true)));
        } else {
            setLastUpdatedLabel("");
        }
    }

    public long getLastUpdated() {
        return mLastUpdated;
    }

}
