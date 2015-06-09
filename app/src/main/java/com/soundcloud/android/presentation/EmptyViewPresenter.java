package com.soundcloud.android.presentation;

import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.lightcycle.LightCycleBinder;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import rx.Subscriber;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public abstract class EmptyViewPresenter extends SupportFragmentLightCycleDispatcher<Fragment> {

    private EmptyView emptyView;
    private int emptyViewStatus = EmptyView.Status.WAITING;

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        LightCycleBinder.bind(this);
        super.onCreate(fragment, bundle);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
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

    public EmptyView getEmptyView() {
        return emptyView;
    }

    private void updateEmptyViewStatus(int status) {
        this.emptyViewStatus = status;
        emptyView.setStatus(status);
    }

    protected final class EmptyViewSubscriber extends Subscriber<Object> {

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
            updateEmptyViewStatus(ErrorUtils.emptyViewStatusFromError(error));
        }
    }
}
