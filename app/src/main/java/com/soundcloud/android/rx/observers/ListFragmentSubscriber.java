package com.soundcloud.android.rx.observers;

import com.soundcloud.android.api.http.APIRequestException;
import com.soundcloud.android.view.EmptyListView;
import rx.Subscriber;

/**
 * Base observer class meant to be used in list fragments which control an {@link EmptyListView}. It automatically puts
 * it in success or errors states based on the outcome of the observable call.
 */
public class ListFragmentSubscriber<T> extends Subscriber<T> {

    private final EmptyViewAware mEmptyViewHolder;

    public ListFragmentSubscriber(EmptyViewAware emptyViewHolder) {
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
