package com.soundcloud.android.view;

import com.soundcloud.android.api.http.APIRequestException;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.view.View;

import javax.inject.Inject;

public class EmptyViewController {

    private EmptyListView emptyView;
    private int emptyViewStatus = EmptyListView.Status.WAITING;

    private Subscription subscription = Subscriptions.empty();

    @Inject
    public EmptyViewController() {
    }

    public <OT extends ConnectableObservable<?>> void onViewCreated(
            final ReactiveComponent<OT> reactiveComponent, OT activeObservable, View view) {
        emptyView = (EmptyListView) view.findViewById(android.R.id.empty);
        emptyView.setStatus(emptyViewStatus);
        emptyView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                updateEmptyViewStatus(EmptyListView.Status.WAITING);
                final OT retryObservable = reactiveComponent.buildObservable();
                retryObservable.subscribe(new EmptyViewSubscriber());
                reactiveComponent.connectObservable(retryObservable);
            }
        });

        subscription = activeObservable.subscribe(new EmptyViewSubscriber());
    }

    public void onDestroyView() {
        subscription.unsubscribe();
        emptyView = null;
    }

    public EmptyListView getEmptyView() {
        return emptyView;
    }

    private void updateEmptyViewStatus(int status) {
        this.emptyViewStatus = status;
        emptyView.setStatus(status);
    }

    private final class EmptyViewSubscriber extends DefaultSubscriber {
        @Override
        public void onCompleted() {
            updateEmptyViewStatus(EmptyListView.Status.OK);
        }

        @Override
        public void onError(Throwable error) {
            if (error instanceof APIRequestException) {
                boolean commsError = ((APIRequestException) error).reason() == APIRequestException.APIErrorReason.NETWORK_COMM_ERROR;
                updateEmptyViewStatus(commsError ? EmptyListView.Status.CONNECTION_ERROR : EmptyListView.Status.SERVER_ERROR);
            } else {
                updateEmptyViewStatus(EmptyListView.Status.ERROR);
            }
        }
    }
}
