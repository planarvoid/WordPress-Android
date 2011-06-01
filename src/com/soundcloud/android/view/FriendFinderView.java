package com.soundcloud.android.view;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;

public class FriendFinderView extends ScTabView {

    public FriendFinderView(ScActivity activity, LazyEndlessAdapter adpWrap) {
        super(activity, adpWrap);
    }

    @Override
    public void onRefresh() {
        if (adapter instanceof LazyEndlessAdapter) {
            ((LazyEndlessAdapter) adapter).refresh();
        }
    }

}
