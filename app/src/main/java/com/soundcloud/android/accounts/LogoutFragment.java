package com.soundcloud.android.accounts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.offline.OfflineContentService;
import com.soundcloud.android.rx.observers.DefaultCompletableObserver;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.disposables.CompositeDisposable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class LogoutFragment extends Fragment {

    @Inject EventBusV2 eventBus;
    @Inject AccountOperations accountOperations;
    @Inject FeatureOperations featureOperations;
    @Inject LeakCanaryWrapper leakCanaryWrapper;

    private final CompositeDisposable disposable = new CompositeDisposable();

    public LogoutFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    LogoutFragment(EventBusV2 eventBus, AccountOperations accountOperations, FeatureOperations featureOperations, LeakCanaryWrapper leakCanaryWrapper) {
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.featureOperations = featureOperations;
        this.leakCanaryWrapper = leakCanaryWrapper;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (featureOperations.isOfflineContentEnabled()) {
            OfflineContentService.stop(getActivity());
        }
        disposable.add(eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new EventObserver()));
        disposable.add(accountOperations.logout().subscribeWith(new LogoutObserver()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fullscreen_progress, container, false);
    }

    @Override
    public void onDestroy() {
        disposable.clear();
        super.onDestroy();
        leakCanaryWrapper.watch(this);
    }

    private final class LogoutObserver extends DefaultCompletableObserver {

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            final Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.finish();
            }
        }
    }

    private final class EventObserver extends DefaultObserver<CurrentUserChangedEvent> {
        @Override
        public void onNext(CurrentUserChangedEvent currentUserChangedEvent) {
            final Activity activity = getActivity();
            if (currentUserChangedEvent.isUserRemoved() && activity != null && !activity.isFinishing()) {
                accountOperations.triggerLoginFlow(activity);
                activity.finish();
            }
        }
    }
}
