
package com.soundcloud.android.view;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;

import android.view.View;
import android.widget.FrameLayout;

public class ScTabView extends FrameLayout {
    public ScListView mListView;
    protected ScListActivity mActivity;

    public ScTabView(ScListActivity activity) {
        super(activity);
        mActivity = activity;

        setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
    }

    public ScTabView(ScListActivity activity, ScListView lv) {
        this(activity);
        mListView = lv;
    }

    public ScListView setLazyListView(ScListView lv, LazyEndlessAdapter adpWrap, int listId, boolean refreshEnabled) {
        mListView = lv;
        addView(lv);
        adpWrap.configureViews(lv);
        if (listId != -1) lv.setId(listId);
        lv.setAdapter(adpWrap, refreshEnabled);
        return lv;
    }

    public void onVisible() {
        if (mListView != null){
            mListView.setVisibility(View.VISIBLE);
            mListView.getWrapper().allowInitialLoading();
            mListView.getWrapper().onResume();
        }
    }

    public void setToTop(){
        if (mListView != null){
            mListView.post(new Runnable() {
                @Override
                public void run() {
                    mListView.getRefreshableView().setSelection(0);
                }
            });
        }
    }
}
