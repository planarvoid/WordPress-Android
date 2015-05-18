package com.soundcloud.android.view.adapters;

import com.soundcloud.android.R;

import android.content.Context;
import android.view.View;

public class ProgressCellPresenter {

    private final int layoutResId;
    private View.OnClickListener retryListener;

    public ProgressCellPresenter(int layoutResId) {
        this.layoutResId = layoutResId;
    }

    public View createView(Context context) {
        return View.inflate(context, layoutResId, null);
    }

    public void bindView(View itemView, boolean wasError) {
        if (wasError) {
            itemView.setBackgroundResource(R.drawable.list_selector_gray);
            itemView.findViewById(R.id.list_loading_view).setVisibility(View.GONE);
            itemView.findViewById(R.id.list_loading_retry_view).setVisibility(View.VISIBLE);
            itemView.setOnClickListener(retryListener);
        } else {
            itemView.setBackgroundResource(android.R.color.transparent);
            itemView.findViewById(R.id.list_loading_view).setVisibility(View.VISIBLE);
            itemView.findViewById(R.id.list_loading_retry_view).setVisibility(View.GONE);
            itemView.setOnClickListener(null);
        }

    }

    public void setRetryListener(View.OnClickListener retryListener) {
        this.retryListener = retryListener;
    }
}
