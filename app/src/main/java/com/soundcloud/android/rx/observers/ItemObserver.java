package com.soundcloud.android.rx.observers;

import com.soundcloud.android.fragment.behavior.AdapterViewAware;
import com.soundcloud.android.view.EmptyListView;

import android.support.v4.app.Fragment;

public class ItemObserver<ModelType, FragmentType extends Fragment & AdapterViewAware<ModelType>>
        extends ScFragmentObserver<FragmentType, ModelType> {

    public ItemObserver(FragmentType fragment) {
        super(fragment);
    }

    @Override
    public void onCompleted(FragmentType fragment) {
        fragment.setEmptyViewStatus(EmptyListView.Status.OK);
        fragment.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onError(FragmentType fragment, Exception error) {
        fragment.setEmptyViewStatus(EmptyListView.Status.ERROR);
    }

    @Override
    public void onNext(FragmentType fragment, ModelType item) {
        fragment.getAdapter().addItem(item);
    }
}
