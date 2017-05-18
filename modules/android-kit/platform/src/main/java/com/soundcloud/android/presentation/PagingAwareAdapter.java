package com.soundcloud.android.presentation;

import android.view.View;

public interface PagingAwareAdapter<ItemT> extends ItemAdapter<ItemT> {

    void setLoading();

    boolean isIdle();

    void setOnErrorRetryListener(View.OnClickListener onErrorRetryListener);
}
