package com.soundcloud.android.view;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

@Deprecated // use ListPresenter or EmptyViewPresenter
public class EmptyViewController extends DefaultSupportFragmentLightCycle {

    private final NetworkConnectionHelper networkConnectionHelper;
    private EmptyView emptyView;
    private int emptyViewStatus = EmptyView.Status.WAITING;

    private Subscription subscription = Subscriptions.empty();

    @Inject
    public EmptyViewController(NetworkConnectionHelper networkConnectionHelper) {
        this.networkConnectionHelper = networkConnectionHelper;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        emptyView = (EmptyView) view.findViewById(android.R.id.empty);
        emptyView.setStatus(emptyViewStatus);
    }

    public <O extends Observable<?>> void connect(final ReactiveComponent<O> reactiveComponent, O activeObservable) {
        emptyView.setOnRetryListener(new EmptyView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                updateEmptyViewStatus(EmptyView.Status.WAITING);
                final O retryObservable = reactiveComponent.buildObservable();
                subscription = retryObservable.subscribe(new EmptyViewSubscriber());
                reactiveComponent.connectObservable(retryObservable);
            }
        });

        subscription = activeObservable.subscribe(new EmptyViewSubscriber());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
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
        public void onNext(Object args) {
            updateEmptyViewStatus(EmptyView.Status.OK);
        }

        @Override
        public void onError(Throwable error) {
            error.printStackTrace();
            if (error instanceof ApiRequestException) {
                boolean networkError = ((ApiRequestException) error).reason() == ApiRequestException.Reason.NETWORK_ERROR;
                updateEmptyViewStatus(networkError ? EmptyView.Status.CONNECTION_ERROR : EmptyView.Status.SERVER_ERROR);
            } if (error instanceof SyncFailedException) {
                // default Sync Failures to connection for now as we can't tell the diff
                updateEmptyViewStatus(networkConnectionHelper.isNetworkConnected()
                        ? EmptyView.Status.SERVER_ERROR : EmptyView.Status.CONNECTION_ERROR);
            } else {
                super.onError(error);
            }
        }
    }
}
