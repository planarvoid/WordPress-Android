package com.soundcloud.android.activities;

import static com.soundcloud.android.testsupport.fixtures.PlayableFixtures.activityTrackLike;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.android.sync.timeline.TimelineOperationsTest;
import rx.Scheduler;

import java.util.Date;
import java.util.List;

public class ActivitiesOperationsTest extends TimelineOperationsTest<ActivityItem, ActivityItem, ActivitiesStorage> {

    @Override
    protected TimelineOperations<ActivityItem, ActivityItem> buildOperations(ActivitiesStorage storage,
                                                                             SyncInitiator syncInitiator,
                                                                             Scheduler scheduler,
                                                                             SyncStateStorage syncStateStorage) {
        return new ActivitiesOperations(storage, syncInitiator, scheduler, syncStateStorage);
    }

    @Override
    protected ActivitiesStorage provideStorageMock() {
        return mock(ActivitiesStorage.class);
    }

    @Override
    protected Syncable provideSyncable() {
        return Syncable.ACTIVITIES;
    }

    @Override
    protected List<ActivityItem> createItems(int length, long lastItemTimestamp) {
        return createStorageModels(length, lastItemTimestamp);
    }

    @Override
    protected ActivityItem createTimelineItem(long timestamp) {
        return activityTrackLike(new Date(timestamp));
    }

    @Override
    protected List<ActivityItem> viewModelsFromStorageModel(List<ActivityItem> items) {
        return items;
    }

    @Override
    protected void initViewModelFromStorageModel(List<ActivityItem> streamEntities, List<ActivityItem> activityItems) {
        //nop
    }
}
