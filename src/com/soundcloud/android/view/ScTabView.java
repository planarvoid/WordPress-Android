
package com.soundcloud.android.view;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;

import static android.view.ViewGroup.LayoutParams.*;

public class ScTabView extends FrameLayout {
    private LazyActivity mActivity;

    private ListAdapter mAdapter;

    public ScTabView(LazyActivity l) {
        super(l);
        mActivity = l;
        setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
    }

    public ScTabView(LazyActivity l, ListAdapter adpWrap) {
        this(l);
        mAdapter = adpWrap;
    }


    public void onStart() {
        // WTF
        if (mAdapter instanceof TracklistAdapter) {
            ((TracklistAdapter) mAdapter).setPlayingId(mActivity.getCurrentTrackId());
        } else if (mAdapter instanceof LazyEndlessAdapter) {
            if (((LazyEndlessAdapter) mAdapter).getWrappedAdapter().getCount() > 0)
                if (((LazyEndlessAdapter) mAdapter).getWrappedAdapter() instanceof TracklistAdapter)
                    ((TracklistAdapter) ((LazyEndlessAdapter) mAdapter).getWrappedAdapter())
                            .setPlayingId(mActivity.getCurrentTrackId());
        }
    }

    public void onStop() {
    }

    public void onAuthenticated() {
    }

    public void onReauthenticate() {
    }

    public void onRefresh(boolean all) {
        if (mAdapter instanceof LazyEndlessAdapter) {
            ((LazyEndlessAdapter) mAdapter).clear();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
    }
}
