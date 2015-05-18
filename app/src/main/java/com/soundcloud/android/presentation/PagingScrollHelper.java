package com.soundcloud.android.presentation;

import com.soundcloud.android.view.adapters.PagingAwareAdapter;

public class PagingScrollHelper {

    private final CollectionViewPresenter presenter;
    private final PagingAwareAdapter adapter;

    public PagingScrollHelper(PagingAwareAdapter adapter, CollectionViewPresenter presenter) {
        this.presenter = presenter;
        this.adapter = adapter;
    }
}
