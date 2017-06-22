package com.soundcloud.android.likes;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.android.rx.observers.LambdaSingleObserver;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class LikesStateProvider {

    private final LikesStorage likesStorage;
    private final BehaviorSubject<LikedStatuses> statuses = BehaviorSubject.create();
    private final EventBusV2 eventBus;
    private final Scheduler scheduler;
    private Set<Urn> likes = new HashSet<>();

    @Inject
    public LikesStateProvider(LikesStorage likesStorage,
                              EventBusV2 eventBus,
                              @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.likesStorage = likesStorage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        publishSnapshot();
        likesStorage.loadLikes()
                    .subscribeOn(scheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(LambdaSingleObserver.onNext(likes -> {
                                                               setLikes(likes);
                                                               publishSnapshot();
                                                           }
                    ));

        eventBus.queue(EventQueue.LIKE_CHANGED)
                .subscribe(LambdaObserver.onNext(likesEvent -> {
                                                     updateLikes(likesEvent);
                                                     publishSnapshot();
                                                 }
                ));
    }

    void setLikes(List<Urn> likesList) {
        likes = new HashSet<>(likesList);
    }

    private void updateLikes(LikesStatusEvent args) {
        for (Map.Entry<Urn, LikesStatusEvent.LikeStatus> entry : args.likes().entrySet()) {
            if (entry.getValue().isUserLike()) {
                likes.add(entry.getKey());
            } else {
                likes.remove(entry.getKey());
            }
        }
    }

    private void publishSnapshot() {
        statuses.onNext(LikedStatuses.create(Collections.unmodifiableSet(likes)));
    }

    public Observable<LikedStatuses> likedStatuses() {
        return statuses;
    }

    public LikedStatuses latest() {
        return statuses.getValue();
    }
}
