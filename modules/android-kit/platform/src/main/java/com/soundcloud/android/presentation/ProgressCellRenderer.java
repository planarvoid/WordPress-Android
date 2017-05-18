package com.soundcloud.android.presentation;

import com.soundcloud.androidkit.R;

import android.content.Context;
import android.view.View;

public class ProgressCellRenderer {

    private final int layoutResId;
    private View.OnClickListener retryListener;

    public ProgressCellRenderer(int layoutResId) {
        this.layoutResId = layoutResId;
    }

    public View createView(Context context) {
        return View.inflate(context, layoutResId, null);
    }

    public void bindView(View itemView, boolean wasError) {
        if (wasError) {
            itemView.setBackgroundResource(R.drawable.ak_list_selector_gray);
            itemView.findViewById(R.id.ak_list_progress).setVisibility(View.GONE);
            itemView.findViewById(R.id.ak_list_retry).setVisibility(View.VISIBLE);
            itemView.setOnClickListener(retryListener);
        } else {
            itemView.setBackgroundResource(android.R.color.transparent);
            itemView.findViewById(R.id.ak_list_progress).setVisibility(View.VISIBLE);
            itemView.findViewById(R.id.ak_list_retry).setVisibility(View.GONE);
            itemView.setOnClickListener(null);
        }
    }

    public void setRetryListener(View.OnClickListener retryListener) {
        this.retryListener = retryListener;
    }
}
