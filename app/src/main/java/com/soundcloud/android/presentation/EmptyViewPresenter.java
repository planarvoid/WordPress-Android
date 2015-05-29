package com.soundcloud.android.presentation;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.lightcycle.LightCycleBinder;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.view.EmptyView;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public abstract class EmptyViewPresenter extends SupportFragmentLightCycleDispatcher<Fragment> {

    private EmptyView emptyView;
    private int emptyViewStatus = EmptyView.Status.WAITING;

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        LightCycleBinder.bind(this);
        super.onCreate(fragment, bundle);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        emptyView = (EmptyView) view.findViewById(android.R.id.empty);
        emptyView.setStatus(emptyViewStatus);
        emptyView.setOnRetryListener(new EmptyView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                updateEmptyViewStatus(EmptyView.Status.WAITING);
                onRetry();
            }
        });
    }

    protected abstract void onRetry();

    @Override
    public void onDestroyView(Fragment fragment) {
        emptyView = null;
        super.onDestroyView(fragment);
    }

    protected EmptyView getEmptyView() {
        return emptyView;
    }

    private void updateEmptyViewStatus(int status) {
        this.emptyViewStatus = status;
        emptyView.setStatus(status);
    }

    protected final class EmptyViewSubscriber extends DefaultSubscriber<Object> {

        @Override
        public void onCompleted() {
            updateEmptyViewStatus(EmptyView.Status.OK);
        }

        @Override
        public void onNext(Object unused) {
            updateEmptyViewStatus(EmptyView.Status.OK);
        }

        @Override
        public void onError(Throwable error) {
            error.printStackTrace();
            if (error instanceof ApiRequestException) {
                updateEmptyViewStatus(((ApiRequestException) error).isNetworkError() ? EmptyView.Status.CONNECTION_ERROR : EmptyView.Status.SERVER_ERROR);
            } if (error instanceof SyncFailedException) {
                // default Sync Failures to connection for now as we can't tell the diff
                updateEmptyViewStatus(EmptyView.Status.CONNECTION_ERROR);
            } else {
                super.onError(error);
            }
        }
    }
}
