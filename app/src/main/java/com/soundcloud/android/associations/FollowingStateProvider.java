package com.soundcloud.android.associations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.java.collections.Lists;
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
import java.util.Set;

@Singleton
public class FollowingStateProvider {

    private final UserAssociationStorage userAssociationStorage;
    private final BehaviorSubject<FollowingStatuses> statuses = BehaviorSubject.create();
    private final EventBus eventBus;
    private final Scheduler scheduler;
    private Set<Urn> followings = new HashSet<>();

    @Inject
    public FollowingStateProvider(UserAssociationStorage userAssociationStorage,
                                  EventBus eventBus,
                                  @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.userAssociationStorage = userAssociationStorage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        publishSnapshot();
        userAssociationStorage.followedUserAssociations().map(userAssociations -> Lists.transform(userAssociations, UserAssociation::userUrn))
                              .subscribeOn(scheduler)
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new DefaultSubscriber<List<Urn>>() {
                                  @Override
                                  public void onNext(List<Urn> followings) {
                                      setFollowings(followings);
                                      publishSnapshot();
                                  }
                              });

        eventBus.queue(EventQueue.FOLLOWING_CHANGED)
                .subscribe(new DefaultSubscriber<FollowingStatusEvent>() {
                    @Override
                    public void onNext(FollowingStatusEvent followingStatusEvent) {
                        updateFollowings(followingStatusEvent);
                        publishSnapshot();
                    }
                });
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
        return statuses.asObservable();
    }

    public FollowingStatuses latest() {
        return statuses.getValue();
    }
}
