package com.soundcloud.android.rx.observers;

import com.soundcloud.android.api.http.APIRequestException;
import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.view.EmptyListView;

import android.support.v4.app.Fragment;

public class ListFragmentObserver<FragmentType extends Fragment & EmptyViewAware, ModelType>
        extends DefaultFragmentObserver<FragmentType, ModelType> {

    public ListFragmentObserver(FragmentType fragment) {
        super(fragment);
    }

    @Override
    public void onCompleted(FragmentType fragment) {
        fragment.setEmptyViewStatus(EmptyListView.Status.OK);
    }

    @Override
    public void onError(FragmentType fragment, Throwable error) {
        if (error instanceof APIRequestException){
            boolean commsError= ((APIRequestException) error).reason() == APIRequestException.APIErrorReason.NETWORK_COMM_ERROR;
            fragment.setEmptyViewStatus(commsError ? EmptyListView.Status.CONNECTION_ERROR : EmptyListView.Status.SERVER_ERROR);
        } else {
            fragment.setEmptyViewStatus(EmptyListView.Status.ERROR);
        }
    }
}
