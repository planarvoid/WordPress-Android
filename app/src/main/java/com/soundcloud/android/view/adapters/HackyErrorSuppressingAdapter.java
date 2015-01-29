package com.soundcloud.android.view.adapters;

import com.soundcloud.android.utils.ErrorUtils;

/**
 * This class exists to suppress errors being reported to the adapter on synced collections. This should be removed
 * once we solve https://github.com/soundcloud/SoundCloud-Android/issues/2743
 */
public class HackyErrorSuppressingAdapter<T> extends EndlessAdapter<T> {
    public HackyErrorSuppressingAdapter(CellPresenter<T> cellPresenter) {
        super(cellPresenter);
    }

    @Override
    public void onError(Throwable e) {
        // we never want to report errors in the append logic for synced collections
        ErrorUtils.handleThrowable(e, getClass());
    }
}
