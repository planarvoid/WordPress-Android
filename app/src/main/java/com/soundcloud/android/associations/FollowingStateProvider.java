package com.soundcloud.android.associations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.android.rx.observers.LambdaSingleObserver;
import com.soundcloud.android.users.Following;
import com.soundcloud.android.users.FollowingStorage;
import com.soundcloud.java.collections.Lists;
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
import java.util.Set;

@Singleton
public class FollowingStateProvider {

    private final FollowingStorage followingStorage;
    private final BehaviorSubject<FollowingStatuses> statuses = BehaviorSubject.create();
    private final EventBusV2 eventBus;
    private final Scheduler scheduler;
    private Set<Urn> followings = new HashSet<>();

    @SuppressLint("sc.MissingCompositeDisposableRecycle") // disposable tied to app lifecycle
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public FollowingStateProvider(FollowingStorage followingStorage,
                                  EventBusV2 eventBus,
                                  @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.followingStorage = followingStorage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        publishSnapshot();

        compositeDisposable.addAll(
                followingStorage.followings().map(userAssociations -> Lists.transform(userAssociations, Following::getUserUrn))
                                .subscribeOn(scheduler)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(LambdaSingleObserver.onSuccess(followings -> {
                                          setFollowings(followings);
                                          publishSnapshot();
                                      })),
                eventBus.queue(EventQueue.FOLLOWING_CHANGED)
                        .subscribeWith(LambdaObserver.onNext(followingStatusEvent -> {
                                                                 updateFollowings(followingStatusEvent);
                                                                 publishSnapshot();
                                                             }
                        ))
        );
    }

    void setFollowings(List<Urn> likesList) {
        followings = new HashSet<>(likesList);
    }

    private void updateFollowings(FollowingStatusEvent entry) {
        if (entry.isFollowed()) {
            followings.add(entry.urn());
        } else {
            followings.remove(entry.urn());
        }
    }

    private void publishSnapshot() {
        statuses.onNext(FollowingStatuses.create(Collections.unmodifiableSet(followings)));
    }

    public Observable<FollowingStatuses> followingStatuses() {
        return statuses;
    }
}
