package com.soundcloud.android.accounts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.offline.OfflineContentService;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.subscriptions.CompositeSubscription;

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

    @Inject EventBus eventBus;
    @Inject AccountOperations accountOperations;
    @Inject FeatureOperations featureOperations;

    private final CompositeSubscription subscription = new CompositeSubscription();

    public LogoutFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    LogoutFragment(EventBus eventBus, AccountOperations accountOperations, FeatureOperations featureOperations) {
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.featureOperations = featureOperations;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (featureOperations.isOfflineContentEnabled()) {
            OfflineContentService.stop(getActivity());
        }
        subscription.add(eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new EventSubscriber()));
        subscription.add(accountOperations.logout().subscribe(new LogoutSubscriber()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fullscreen_progress, container, false);
    }

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

    private final class LogoutSubscriber extends DefaultSubscriber<Void> {

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            final Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                activity.finish();
            }
        }
    }

    private final class EventSubscriber extends DefaultSubscriber<CurrentUserChangedEvent> {
        @Override
        public void onNext(CurrentUserChangedEvent currentUserChangedEvent) {
            final Activity activity = getActivity();
            if (currentUserChangedEvent.getKind() == CurrentUserChangedEvent.USER_REMOVED
                    && activity != null && !activity.isFinishing()) {
                accountOperations.triggerLoginFlow(activity);
                activity.finish();
            }
        }
    }
}
