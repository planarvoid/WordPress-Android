package com.soundcloud.android.view.adapters;

import android.view.View;

public interface PagingAwareAdapter<ItemT> extends ItemAdapter<ItemT> {

    int PROGRESS_VIEW_TYPE = -1;

    void setLoading();

    boolean isIdle();

    void setOnErrorRetryListener(View.OnClickListener onErrorRetryListener);
}
