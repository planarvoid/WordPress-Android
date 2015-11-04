package com.soundcloud.android.activities;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

class ActivitiesOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

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

    private final ActivitiesStorage activitiesStorage;
    private final Scheduler scheduler;

    @Inject
    ActivitiesOperations(ActivitiesStorage activitiesStorage,
                         @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.activitiesStorage = activitiesStorage;
        this.scheduler = scheduler;
    }

    Observable<List<ActivityItem>> initialActivities() {
        return activitiesStorage.initialActivityItems(PAGE_SIZE)
                .subscribeOn(scheduler)
                .toList()
                .map(TO_VIEW_MODELS);
    }

    Pager.PagingFunction<List<ActivityItem>> pagingFunc() {
        return new Pager.PagingFunction<List<ActivityItem>>() {
            @Override
            public Observable<List<ActivityItem>> call(List<ActivityItem> activityItems) {
                return Pager.finish();
            }
        };
    }

}
