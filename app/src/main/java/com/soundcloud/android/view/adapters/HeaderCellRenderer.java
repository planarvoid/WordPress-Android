package com.soundcloud.android.view.adapters;

import android.content.Context;
import android.view.View;

public class HeaderCellRenderer {

    private final int layoutResId;

    public HeaderCellRenderer(int layoutResId) {
        this.layoutResId = layoutResId;
    }

    public View createView(Context context) {
        return View.inflate(context, layoutResId, null);
    }
}
