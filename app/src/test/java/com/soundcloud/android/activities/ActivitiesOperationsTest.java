package com.soundcloud.android.activities;

import static com.soundcloud.android.activities.ActivitiesOperations.PAGE_SIZE;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.activityTrackLike;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;

// AndroidUnitTest because of PropertySet
public class ActivitiesOperationsTest extends AndroidUnitTest {

    private ActivitiesOperations operations;

    @Mock private ActivitiesStorage storage;
    private TestSubscriber<List<ActivityItem>> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new ActivitiesOperations(storage, Schedulers.immediate());
    }

    @Test
    public void shouldLoadFirstPageOfItemsFromLocalStorage() {
        final PropertySet activityTrackLike = activityTrackLike();
        when(storage.initialActivityItems(PAGE_SIZE)).thenReturn(just(activityTrackLike));

        operations.initialActivities().subscribe(subscriber);

        subscriber.assertValue(singletonList(new ActivityItem(activityTrackLike)));
    }
}
