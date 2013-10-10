package com.soundcloud.android.rx.observers;

import com.soundcloud.android.api.http.APIRequestException;
import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.view.EmptyListView;
import rx.Observer;

public class ListFragmentObserver<T> implements Observer<T> {

    private final EmptyViewAware mEmptyViewHolder;

    public ListFragmentObserver(EmptyViewAware emptyViewHolder) {
        mEmptyViewHolder = emptyViewHolder;
    }

    @Override
    public void onCompleted() {
        mEmptyViewHolder.setEmptyViewStatus(EmptyListView.Status.OK);
    }

    @Override
    public void onError(Throwable error) {
        if (error instanceof APIRequestException){
            boolean commsError= ((APIRequestException) error).reason() == APIRequestException.APIErrorReason.NETWORK_COMM_ERROR;
            mEmptyViewHolder.setEmptyViewStatus(commsError ? EmptyListView.Status.CONNECTION_ERROR : EmptyListView.Status.SERVER_ERROR);
        } else {
            mEmptyViewHolder.setEmptyViewStatus(EmptyListView.Status.ERROR);
        }
    }

    @Override
    public void onNext(T item) {
    }
}
