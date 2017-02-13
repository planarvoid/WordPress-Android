package com.soundcloud.android.activities;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

class ActivitiesOperations extends TimelineOperations<ActivityItem, ActivityItem> {

    private final Scheduler scheduler;

    @Inject
    ActivitiesOperations(ActivitiesStorage activitiesStorage,
                         SyncInitiator syncInitiator,
                         @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                         SyncStateStorage syncStateStorage) {
        super(Syncable.ACTIVITIES,
              activitiesStorage,
              syncInitiator,
              scheduler,
              syncStateStorage);
        this.scheduler = scheduler;
    }

    Observable<List<ActivityItem>> initialActivities() {
        return initialTimelineItems(false);
    }

    Observable<List<ActivityItem>> updatedActivities() {
        return updatedTimelineItems().subscribeOn(scheduler);
    }

    @Override
    protected boolean isEmptyResult(List<ActivityItem> result) {
        return result.isEmpty();
    }

    @Override
    protected Observable<List<ActivityItem>> toViewModels(List<ActivityItem> activityItems) {
        return Observable.just(activityItems);
    }

    public Optional<Date> getFirstItemTimestamp(List<ActivityItem> items) {
        final ListIterator<ActivityItem> iterator = items.listIterator();
        if (iterator.hasNext()) {
            return Optional.of(iterator.next().getCreatedAt());
        }
        return Optional.absent();
    }

    protected Optional<Date> getLastItemTimestamp(List<ActivityItem> items) {
        final ListIterator<ActivityItem> iterator = items.listIterator(items.size());
        if (iterator.hasPrevious()) {
            return Optional.of(iterator.previous().getCreatedAt());
        }
        return Optional.absent();
    }
}
