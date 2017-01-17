package com.soundcloud.android.likes;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;

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
    private final EventBus eventBus;
    private final Scheduler scheduler;
    private Set<Urn> likes= new HashSet<>();

    @Inject
    public LikesStateProvider(LikesStorage likesStorage,
                              EventBus eventBus,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.likesStorage = likesStorage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        publishSnapshot();
        likesStorage.loadLikes()
                .subscribeOn(scheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultSubscriber<List<Urn>>(){
                    @Override
                    public void onNext(List<Urn> likes) {
                        setLikes(likes);
                        publishSnapshot();
                    }
                });

        eventBus.queue(EventQueue.LIKE_CHANGED)
                .subscribe(new DefaultSubscriber<LikesStatusEvent>(){
                    @Override
                    public void onNext(LikesStatusEvent likesEvent) {
                        updateLikes(likesEvent);
                        publishSnapshot();
                    }
                });
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
        statuses.onNext(new LikedStatuses(Collections.unmodifiableSet(likes)));
    }

    public Observable<LikedStatuses> likedStatuses() {
        return statuses.asObservable();
    }
}
