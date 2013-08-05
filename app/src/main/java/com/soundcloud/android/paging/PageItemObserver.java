package com.soundcloud.android.paging;

import com.soundcloud.android.fragment.behavior.PagingAdapterViewAware;
import com.soundcloud.android.rx.observers.ScFragmentObserver;
import com.soundcloud.android.view.EmptyListView;

import android.support.v4.app.Fragment;

public class PageItemObserver<ModelType, FragmentType extends Fragment & PagingAdapterViewAware<ModelType>>
        extends ScFragmentObserver<FragmentType, ModelType> {

    public PageItemObserver(FragmentType fragment) {
        super(fragment);
    }

    @Override
    public void onCompleted(FragmentType fragment) {
        fragment.getEmptyView().setStatus(EmptyListView.Status.OK);
        fragment.getAdapter().setDisplayProgressItem(false);
    }

    @Override
    public void onError(FragmentType fragment, Exception error) {
        fragment.getEmptyView().setStatus(EmptyListView.Status.ERROR);
    }

    @Override
    public void onNext(FragmentType fragment, ModelType item) {
        fragment.getAdapter().addItem(item);
    }
}
