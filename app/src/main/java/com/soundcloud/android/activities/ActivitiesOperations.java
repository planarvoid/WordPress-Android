package com.soundcloud.android.activities;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.sync.SyncContent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

class ActivitiesOperations extends TimelineOperations<ActivityItem> {

    private static final Func1<List<PropertySet>, List<ActivityItem>> TO_VIEW_MODELS =
            new Func1<List<PropertySet>, List<ActivityItem>>() {
        @Override
        public List<ActivityItem> call(List<PropertySet> propertySets) {
            final List<ActivityItem> items = new ArrayList<>(propertySets.size());
            for (PropertySet sourceSet : propertySets) {
                items.add(new ActivityItem(sourceSet));
            }
            return items;
        }
    };

    private final Scheduler scheduler;

    @Inject
    ActivitiesOperations(ActivitiesStorage activitiesStorage,
                         SyncInitiator syncInitiator,
                         ContentStats contentStats,
                         @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                         SyncStateStorage syncStateStorage) {
        super(SyncContent.MyActivities, activitiesStorage, syncInitiator, contentStats, scheduler, syncStateStorage);
        this.scheduler = scheduler;
    }

    Observable<List<ActivityItem>> initialActivities() {
        return initialTimelineItems(false);
    }

    @Override
    protected Observable<List<ActivityItem>> initialTimelineItems(boolean syncCompleted) {
        return super.initialTimelineItems(syncCompleted).subscribeOn(scheduler);
    }

    Observable<List<ActivityItem>> updatedActivities() {
        return updatedTimelineItems().subscribeOn(scheduler);
    }

    @Override
    protected Func1<List<PropertySet>, List<ActivityItem>> toViewModels() {
        return TO_VIEW_MODELS;
    }

    @Override
    protected boolean isEmptyResult(List<ActivityItem> result) {
        return result.isEmpty();
    }
}
