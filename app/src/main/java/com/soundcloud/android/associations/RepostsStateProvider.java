package com.soundcloud.android.associations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RepostsStateProvider {

    private final RepostStorage repostStorage;
    private final BehaviorSubject<RepostStatuses> statuses = BehaviorSubject.create();
    private final EventBus eventBus;
    private final Scheduler scheduler;
    private Set<Urn> reposts = new HashSet<>();

    @Inject
    public RepostsStateProvider(RepostStorage repostStorage,
                                EventBus eventBus,
                                @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.repostStorage = repostStorage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        repostStorage.loadReposts()
                     .subscribeOn(scheduler)
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(new DefaultSubscriber<List<Urn>>(){
                    @Override
                    public void onNext(List<Urn> reposts) {
                        setReposts(reposts);
                        publishSnapshot();
                    }
                });

        eventBus.queue(EventQueue.REPOST_CHANGED)
                .subscribe(new DefaultSubscriber<RepostsStatusEvent>(){
                    @Override
                    public void onNext(RepostsStatusEvent repostsStatusEvent) {
                        updateReposts(repostsStatusEvent);
                        publishSnapshot();
                    }
                });
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
        statuses.onNext(new RepostStatuses(Collections.unmodifiableSet(reposts)));
    }

    public Observable<RepostStatuses> repostedStatuses() {
        return statuses.asObservable();
    }
}
