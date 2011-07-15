
package com.soundcloud.android.view;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

import android.util.Log;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;

import android.widget.FrameLayout;
import android.widget.ListAdapter;

public class ScTabView extends FrameLayout {
    public LazyListView mListView;
    protected ScActivity mActivity;

    public ScTabView(ScActivity activity) {
        super(activity);
        mActivity = activity;

        setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
    }

    public ScTabView(ScActivity activity, LazyListView lv) {
        this(activity);
        mListView = lv;
    }

     public void onRefresh(boolean userRefresh) {
        if (mListView != null) {
            mListView.setSelection(0);
            mListView.onRefresh();
        }
    }

    public LazyListView setLazyListView(LazyListView lv, LazyEndlessAdapter adpWrap, int listId, boolean refreshEnabled) {
        mListView = lv;
        addView(lv);
        adpWrap.configureViews(lv);
        if (listId != -1) lv.setId(listId);
        lv.setAdapter(adpWrap, refreshEnabled);
        return lv;
    }

}
