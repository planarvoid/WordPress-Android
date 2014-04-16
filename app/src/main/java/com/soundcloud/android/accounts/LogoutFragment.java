package com.soundcloud.android.accounts;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class LogoutFragment extends Fragment {

    @Inject
    EventBus eventBus;
    @Inject
    AccountOperations accountOperations;

    private final CompositeSubscription subscription = new CompositeSubscription();

    public LogoutFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    LogoutFragment(EventBus eventBus, AccountOperations accountOperations) {
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subscription.add(eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new EventSubscriber()));
        subscription.add(accountOperations.removeSoundCloudAccount().subscribe(new LogoutSubscriber()));
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
                accountOperations.addSoundCloudAccountManually(activity);
                activity.finish();
            }
        }
    }
}
