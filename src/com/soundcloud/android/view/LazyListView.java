
package com.soundcloud.android.view;

import android.content.Context;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.soundcloud.android.adapter.LazyEndlessAdapter;


// XXX remove
public class LazyListView extends ListView {
    public LazyListView(Context context) {
        super(context);
    }

    @Override
    public ListAdapter getAdapter() {
        if (super.getAdapter() instanceof LazyEndlessAdapter) {
            return ((LazyEndlessAdapter) super.getAdapter()).getWrappedAdapter();
        } else
            return super.getAdapter();
    }

    public LazyEndlessAdapter getWrapper() {
        if (super.getAdapter() instanceof LazyEndlessAdapter) {
            return (LazyEndlessAdapter) super.getAdapter();
        } else
            return null;
    }

    @Override
    protected void layoutChildren() {
        try {
            super.layoutChildren();
        } catch (Exception ignored) {

        }
    }
}
