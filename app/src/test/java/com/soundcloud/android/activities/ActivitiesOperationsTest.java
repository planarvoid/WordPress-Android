package com.soundcloud.android.activities;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.activityTrackLike;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.sync.SyncContent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.android.sync.timeline.TimelineOperationsTest;
import com.soundcloud.java.collections.PropertySet;
import rx.Scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ActivitiesOperationsTest extends TimelineOperationsTest<ActivityItem, ActivitiesStorage> {

    @Override
    protected TimelineOperations<ActivityItem> buildOperations(ActivitiesStorage storage,
                                                               SyncInitiator syncInitiator,
                                                               ContentStats contentStats,
                                                               Scheduler scheduler,
                                                               SyncStateStorage syncStateStorage) {
        return new ActivitiesOperations(storage, syncInitiator, contentStats, scheduler, syncStateStorage);
    }

    @Override
    protected ActivitiesStorage provideStorageMock() {
        return mock(ActivitiesStorage.class);
    }

    @Override
    protected SyncContent provideSyncContent() {
        return SyncContent.MyActivities;
    }

    @Override
    protected PropertySet createTimelineItem(long timestamp) {
        return activityTrackLike().put(ActivityProperty.DATE, new Date(timestamp));
    }

    @Override
    protected List<ActivityItem> viewModelsFromPropertySets(List<PropertySet> source) {
        List<ActivityItem> items = new ArrayList<>(source.size());
        for (PropertySet item : source) {
            items.add(new ActivityItem(item));
        }
        return items;
    }

}
