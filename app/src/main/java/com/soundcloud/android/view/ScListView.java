package com.soundcloud.android.view;

import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;

public class ScListView extends PullToRefreshListView {

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
    }

    @Override
    public int getSolidColor() {
        return 0x666666;
    }
}
