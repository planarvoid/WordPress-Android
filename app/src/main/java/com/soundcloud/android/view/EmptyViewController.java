package com.soundcloud.android.view;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

/**
 * @deprecated use ListPresenter or EmptyViewPresenter
 */
@Deprecated
public class EmptyViewController extends DefaultSupportFragmentLightCycle<Fragment> {

    private final ConnectionHelper connectionHelper;
    private EmptyView emptyView;
    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public EmptyViewController(ConnectionHelper connectionHelper) {
        this.connectionHelper = connectionHelper;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        emptyView = view.findViewById(android.R.id.empty);
        emptyView.setStatus(emptyViewStatus);
    }

    public <O extends Observable<?>> void connect(O activeObservable) {
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

    private void updateEmptyViewStatus(EmptyView.Status status) {
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
            if (error instanceof ApiRequestException) {
                boolean networkError = ((ApiRequestException) error).isNetworkError();
                updateEmptyViewStatus(networkError ? EmptyView.Status.CONNECTION_ERROR : EmptyView.Status.SERVER_ERROR);
            }
            if (error instanceof SyncFailedException) {
                // default Sync Failures to connection for now as we can't tell the diff
                updateEmptyViewStatus(connectionHelper.isNetworkConnected()
                                      ? EmptyView.Status.SERVER_ERROR : EmptyView.Status.CONNECTION_ERROR);
            } else {
                super.onError(error);
            }
        }
    }
}
