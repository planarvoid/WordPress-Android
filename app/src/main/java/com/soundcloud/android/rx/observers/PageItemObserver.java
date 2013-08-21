package com.soundcloud.android.rx.observers;

import com.soundcloud.android.fragment.behavior.PagingAdapterViewAware;
import com.soundcloud.android.rx.observers.ScFragmentObserver;
import com.soundcloud.android.view.EmptyListView;

import android.support.v4.app.Fragment;

public class PageItemObserver<ModelType, FragmentType extends Fragment & PagingAdapterViewAware<ModelType>>
        extends ItemObserver<ModelType, FragmentType> {

    public PageItemObserver(FragmentType fragment) {
        super(fragment);
    }

    @Override
    public void onCompleted(FragmentType fragment) {
        super.onCompleted(fragment);
        fragment.getAdapter().setDisplayProgressItem(false);
    }

    @Override
    public void onError(FragmentType fragment, Exception error) {
        super.onError(fragment, error);
        fragment.getAdapter().setDisplayProgressItem(false);
    }
}
