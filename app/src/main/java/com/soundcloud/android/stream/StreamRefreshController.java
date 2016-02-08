package com.soundcloud.android.stream;

import static com.soundcloud.android.ApplicationModule.LOW_PRIORITY;
import static com.soundcloud.android.events.StreamEvent.fromStreamRefresh;
import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.rx.RxUtils.returning;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

public class StreamRefreshController extends DefaultActivityLightCycle<AppCompatActivity> {
    private static final long VERIFY_INTERVAL_MS = TimeUnit.SECONDS.toMillis(30);
    private static final long REFRESH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(30);

    private final EventBus eventBus;
    private final SoundStreamOperations operations;
    private final CurrentDateProvider dateProvider;
    private final Scheduler scheduler;
    private final FeatureFlags featureFlags;

    private CompositeSubscription subscription = new CompositeSubscription();

    private final Func1<Long, Boolean> canUpdateStream = new Func1<Long, Boolean>() {
        @Override
        public Boolean call(Long lastSync) {
            return (dateProvider.getCurrentTime() - lastSync) > REFRESH_INTERVAL_MS;
        }
    };

    @Inject
    public StreamRefreshController(EventBus eventBus,
                                   SoundStreamOperations operations,
                                   CurrentDateProvider dateProvider,
                                   @Named(LOW_PRIORITY) Scheduler scheduler,
                                   FeatureFlags featureFlags) {
        this.eventBus = eventBus;
        this.operations = operations;
        this.dateProvider = dateProvider;
        this.scheduler = scheduler;
        this.featureFlags = featureFlags;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        super.onResume(activity);

        if (featureFlags.isEnabled(Flag.AUTO_REFRESH_STREAM)) {
            subscription.add(streamRefreshSubscription());
        }
    }

    private Subscription streamRefreshSubscription() {
        return Observable
                .interval(VERIFY_INTERVAL_MS, TimeUnit.MILLISECONDS, scheduler)
                .flatMap(continueWith(operations.getLastSync()))
                .filter(canUpdateStream)
                .flatMap(continueWith(operations.updatedStreamItems()))
                .map(returning(fromStreamRefresh()))
                .subscribe(eventBus.queue(EventQueue.STREAM));
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        subscription.clear();
        super.onPause(activity);
    }
}
