package com.soundcloud.android.associations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.android.rx.observers.LambdaSingleObserver;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

import android.annotation.SuppressLint;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class RepostsStateProvider {

    private final RepostStorage repostStorage;
    private final BehaviorSubject<RepostStatuses> statuses = BehaviorSubject.create();
    private final EventBusV2 eventBus;
    private final Scheduler scheduler;
    private Set<Urn> reposts = new HashSet<>();

    @SuppressLint("sc.MissingCompositeDisposableRecycle") // disposable tied to app lifecycle
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public RepostsStateProvider(RepostStorage repostStorage,
                                EventBusV2 eventBus,
                                @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.repostStorage = repostStorage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        compositeDisposable.addAll(repostStorage.loadReposts()
                     .subscribeOn(scheduler)
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribeWith(LambdaSingleObserver.onNext(reposts -> {
                                                                setReposts(reposts);
                                                                publishSnapshot();
                                                            }
                     )),

        eventBus.queue(EventQueue.REPOST_CHANGED)
                .subscribeWith(LambdaObserver.onNext(repostsStatusEvent -> {
                                                     updateReposts(repostsStatusEvent);
                                                     publishSnapshot();
                                                 }
                )));
    }

    void setReposts(List<Urn> reposts) {
        this.reposts = new HashSet<>(reposts);
    }

    void updateReposts(RepostsStatusEvent event) {
        for (Map.Entry<Urn, RepostsStatusEvent.RepostStatus> entry : event.reposts().entrySet()) {
            if (entry.getValue().isReposted()) {
                reposts.add(entry.getKey());
            } else {
                reposts.remove(entry.getKey());
            }
        }
    }

    private void publishSnapshot() {
        statuses.onNext(RepostStatuses.create(Collections.unmodifiableSet(reposts)));
    }

    public Observable<RepostStatuses> repostedStatuses() {
        return statuses;
    }
}
