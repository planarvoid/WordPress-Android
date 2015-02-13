package com.soundcloud.android.collections;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.ListView;

@Deprecated
public class ScListView extends ListView {

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

        setFadingEdgeLength((int) (2 * res.getDisplayMetrics().density));
        setSelector(R.drawable.list_selector_gray);
        setLongClickable(false);
        setScrollingCacheEnabled(false);
        setCacheColorHint(Color.WHITE);
    }

    @Override
    public int getSolidColor() {
        return 0x666666;
    }
}
