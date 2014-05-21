package com.soundcloud.android.view;

import com.soundcloud.android.api.http.APIRequestException;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.view.View;

import javax.inject.Inject;

public class EmptyViewController {

    private EmptyView emptyView;
    private int emptyViewStatus = EmptyView.Status.WAITING;

    private Subscription subscription = Subscriptions.empty();

    @Inject
    public EmptyViewController() {
    }

    public <OT extends ConnectableObservable<?>> void onViewCreated(
            final ReactiveComponent<OT> reactiveComponent, OT activeObservable, View view) {
        emptyView = (EmptyView) view.findViewById(android.R.id.empty);
        emptyView.setStatus(emptyViewStatus);
        emptyView.setOnRetryListener(new EmptyView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                updateEmptyViewStatus(EmptyView.Status.WAITING);
                final OT retryObservable = reactiveComponent.buildObservable();
                subscription = retryObservable.subscribe(new EmptyViewSubscriber());
                reactiveComponent.connectObservable(retryObservable);
            }
        });

        subscription = activeObservable.subscribe(new EmptyViewSubscriber());
    }

    public void onDestroyView() {
        subscription.unsubscribe();
        emptyView = null;
    }

    public EmptyView getEmptyView() {
        return emptyView;
    }

    private void updateEmptyViewStatus(int status) {
        this.emptyViewStatus = status;
        emptyView.setStatus(status);
    }

    private final class EmptyViewSubscriber extends DefaultSubscriber {
        @Override
        public void onCompleted() {
            updateEmptyViewStatus(EmptyView.Status.OK);
        }

        @Override
        public void onError(Throwable error) {
            if (error instanceof APIRequestException) {
                boolean commsError = ((APIRequestException) error).reason() == APIRequestException.APIErrorReason.NETWORK_COMM_ERROR;
                updateEmptyViewStatus(commsError ? EmptyView.Status.CONNECTION_ERROR : EmptyView.Status.SERVER_ERROR);
            } else {
                updateEmptyViewStatus(EmptyView.Status.ERROR);
            }
        }
    }
}
