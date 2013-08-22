package com.soundcloud.android.rx.observers;

import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.view.EmptyListView;

import android.support.v4.app.Fragment;

public class ListFragmentObserver<ModelType, FragmentType extends Fragment & EmptyViewAware>
        extends ScFragmentObserver<FragmentType, ModelType> {

    public ListFragmentObserver(FragmentType fragment) {
        super(fragment);
    }

    @Override
    public void onCompleted(FragmentType fragment) {
        fragment.setEmptyViewStatus(EmptyListView.Status.OK);
    }

    @Override
    public void onError(FragmentType fragment, Exception error) {
        fragment.setEmptyViewStatus(EmptyListView.Status.ERROR);
    }
}
