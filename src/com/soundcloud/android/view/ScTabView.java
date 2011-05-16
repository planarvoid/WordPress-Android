
package com.soundcloud.android.view;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

public class ScTabView extends FrameLayout {
    public ListAdapter adapter;

    public ScTabView(Activity l) {
        super(l);
        setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
    }

    public ScTabView(ScActivity l, LazyEndlessAdapter adpWrap) {
        this(l);
        adapter = adpWrap;
    }

    public void onStart() {
        // WTF

        /*
        if (mAdapter instanceof TracklistAdapter) {
            ((TracklistAdapter) mAdapter).setPlayingId(mActivity.getCurrentTrackId());
        } else if (mAdapter instanceof LazyEndlessAdapter) {
            if (((LazyEndlessAdapter) mAdapter).getWrappedAdapter().getCount() > 0)
                if (((LazyEndlessAdapter) mAdapter).getWrappedAdapter() instanceof TracklistAdapter)
                    ((TracklistAdapter) ((LazyEndlessAdapter) mAdapter).getWrappedAdapter())
                            .setPlayingId(mActivity.getCurrentTrackId());
        }
         */
    }


    public void onRefresh() {
        if (adapter instanceof LazyEndlessAdapter) {
            ((LazyEndlessAdapter) adapter).refresh();
        }
    }

    public void onSaveInstanceState(Bundle state) {
    }

    public void onRestoreInstanceState(Bundle state) {
    }

    public void onDestroy(){

    }

}
