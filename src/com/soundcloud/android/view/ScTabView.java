
package com.soundcloud.android.view;

import android.app.Activity;
import android.widget.FrameLayout;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

public class ScTabView extends FrameLayout {
    private LazyEndlessAdapter mAdapter;

    public ScTabView(Activity l) {
        super(l);
        setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
    }

    public ScTabView(ScActivity l, LazyEndlessAdapter adpWrap) {
        this(l);
        mAdapter = adpWrap;
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
        if (mAdapter != null) {
            mAdapter.clear();
        }
    }
}
