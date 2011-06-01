
package com.soundcloud.android.view;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;

import android.widget.FrameLayout;
import android.widget.ListAdapter;

public class ScTabView extends FrameLayout {
    public ListAdapter adapter;
    protected ScActivity mActivity;

    public ScTabView(ScActivity activity) {
        super(activity);
        mActivity = activity;

        setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
    }

    public ScTabView(ScActivity activity, LazyEndlessAdapter adpWrap) {
        this(activity);
        adapter = adpWrap;
    }

    public void onRefresh() {
        if (adapter instanceof LazyEndlessAdapter) {
            ((LazyEndlessAdapter) adapter).refresh();
        }
    }
}
