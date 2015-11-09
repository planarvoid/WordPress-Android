package com.soundcloud.android.activities;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiTrackLikeActivity;

import com.soundcloud.android.sync.activities.ApiTrackLikeActivity;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Date;

public class ActivitiesStorageTest extends StorageIntegrationTest {

    private static final long TIMESTAMP = 1000L;

    private ActivitiesStorage storage;

    private TestSubscriber<PropertySet> subscriber = new TestSubscriber<>();
    private ApiTrackLikeActivity oldestActivity, olderActivity, newestActivity;

    @Before
    public void setUp() throws Exception {
        storage = new ActivitiesStorage(propellerRx());
        oldestActivity = apiTrackLikeActivity(new Date(TIMESTAMP));
        olderActivity = apiTrackLikeActivity(new Date(TIMESTAMP + 1));
        newestActivity = apiTrackLikeActivity(new Date(TIMESTAMP + 2));
        testFixtures().insertTrackLikeActivity(oldestActivity);
        testFixtures().insertTrackLikeActivity(olderActivity);
        testFixtures().insertTrackLikeActivity(newestActivity);
    }

    @Test
    public void shouldLoadLatestActivitiesInReverseChronologicalOrder() {
        storage.timelineItems(Integer.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(
                expectedTrackLikePropertiesFor(newestActivity),
                expectedTrackLikePropertiesFor(olderActivity),
                expectedTrackLikePropertiesFor(oldestActivity)
        );
        subscriber.assertCompleted();
    }

    @Test
    public void shouldLimitLatestActivitiesResultSet() {
        final int limit = 1;
        storage.timelineItems(limit).subscribe(subscriber);

        subscriber.assertValue(expectedTrackLikePropertiesFor(newestActivity));
        subscriber.assertCompleted();
    }

    @Test
    public void shouldLoadActivitiesBeforeGivenTimestampInReverseChronologicalOrder() {
        storage.timelineItemsBefore(TIMESTAMP + 2, Integer.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(
                expectedTrackLikePropertiesFor(olderActivity),
                expectedTrackLikePropertiesFor(oldestActivity)
        );
        subscriber.assertCompleted();
    }

    @Test
    public void shouldLimitActivitiesBeforeGivenTimestamp() {
        final int limit = 1;
        storage.timelineItemsBefore(TIMESTAMP + 2, limit).subscribe(subscriber);

        subscriber.assertValue(expectedTrackLikePropertiesFor(olderActivity));
        subscriber.assertCompleted();
    }

    private PropertySet expectedTrackLikePropertiesFor(ApiTrackLikeActivity activity) {
        return PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.TRACK_LIKE),
                ActivityProperty.DATE.bind(activity.getCreatedAt()),
                ActivityProperty.PLAYABLE_TITLE.bind(activity.getTrack().getTitle()),
                ActivityProperty.USER_NAME.bind(activity.getUser().getUsername()),
                ActivityProperty.USER_URN.bind(activity.getUserUrn())
        );
    }
}
