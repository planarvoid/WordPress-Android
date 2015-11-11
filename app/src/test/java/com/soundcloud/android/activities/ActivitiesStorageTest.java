package com.soundcloud.android.activities;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiTrackCommentActivity;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiTrackLikeActivity;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiUserFollowActivity;

import com.soundcloud.android.sync.activities.ApiTrackCommentActivity;
import com.soundcloud.android.sync.activities.ApiTrackLikeActivity;
import com.soundcloud.android.sync.activities.ApiUserFollowActivity;
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
    private ApiTrackCommentActivity oldestActivity;
    private ApiTrackLikeActivity olderActivity;
    private ApiUserFollowActivity newestActivity;

    @Before
    public void setUp() throws Exception {
        storage = new ActivitiesStorage(propellerRx());
        oldestActivity = apiTrackCommentActivity(new Date(TIMESTAMP));
        olderActivity = apiTrackLikeActivity(new Date(TIMESTAMP + 1));
        newestActivity = apiUserFollowActivity(new Date(TIMESTAMP + 2));
        testFixtures().insertTrackCommentActivity(oldestActivity);
        testFixtures().insertTrackLikeActivity(olderActivity);
        testFixtures().insertUserFollowActivity(newestActivity);
    }

    @Test
    public void shouldLoadLatestActivitiesInReverseChronologicalOrder() {
        storage.timelineItems(Integer.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(
                expectedPropertiesFor(newestActivity),
                expectedPropertiesFor(olderActivity),
                expectedPropertiesFor(oldestActivity)
        );
        subscriber.assertCompleted();
    }

    @Test
    public void shouldNotReturnUnsupportedActivitiesFromLatestActivities() {
        testFixtures().insertUnsupportedActivity();

        storage.timelineItems(Integer.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(
                expectedPropertiesFor(newestActivity),
                expectedPropertiesFor(olderActivity),
                expectedPropertiesFor(oldestActivity)
        );
        subscriber.assertCompleted();
    }

    @Test
    public void shouldLimitLatestActivitiesResultSet() {
        final int limit = 1;
        storage.timelineItems(limit).subscribe(subscriber);

        subscriber.assertValue(expectedPropertiesFor(newestActivity));
        subscriber.assertCompleted();
    }

    @Test
    public void shouldLoadActivitiesBeforeGivenTimestampInReverseChronologicalOrder() {
        storage.timelineItemsBefore(TIMESTAMP + 2, Integer.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(
                expectedPropertiesFor(olderActivity),
                expectedPropertiesFor(oldestActivity)
        );
        subscriber.assertCompleted();
    }

    @Test
    public void shouldNotReturnUnsupportedActivitiesFromActivitiesBeforeGivenTimestamp() {
        testFixtures().insertUnsupportedActivity();

        storage.timelineItemsBefore(Long.MAX_VALUE, Integer.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(
                expectedPropertiesFor(newestActivity),
                expectedPropertiesFor(olderActivity),
                expectedPropertiesFor(oldestActivity)
        );
        subscriber.assertCompleted();
    }

    @Test
    public void shouldLimitActivitiesBeforeGivenTimestamp() {
        final int limit = 1;
        storage.timelineItemsBefore(TIMESTAMP + 2, limit).subscribe(subscriber);

        subscriber.assertValue(expectedPropertiesFor(olderActivity));
        subscriber.assertCompleted();
    }

    private PropertySet expectedPropertiesFor(ApiUserFollowActivity activity) {
        return PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.USER_FOLLOW),
                ActivityProperty.DATE.bind(activity.getCreatedAt()),
                ActivityProperty.USER_NAME.bind(activity.getUser().getUsername()),
                ActivityProperty.USER_URN.bind(activity.getUserUrn())
        );
    }

    private PropertySet expectedPropertiesFor(ApiTrackLikeActivity activity) {
        return PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.TRACK_LIKE),
                ActivityProperty.DATE.bind(activity.getCreatedAt()),
                ActivityProperty.PLAYABLE_TITLE.bind(activity.getTrack().getTitle()),
                ActivityProperty.USER_NAME.bind(activity.getUser().getUsername()),
                ActivityProperty.USER_URN.bind(activity.getUserUrn())
        );
    }

    private PropertySet expectedPropertiesFor(ApiTrackCommentActivity activity) {
        return PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.TRACK_COMMENT),
                ActivityProperty.DATE.bind(activity.getCreatedAt()),
                ActivityProperty.COMMENTED_TRACK_URN.bind(activity.getTrack().getUrn()),
                ActivityProperty.PLAYABLE_TITLE.bind(activity.getTrack().getTitle()),
                ActivityProperty.USER_NAME.bind(activity.getUser().getUsername()),
                ActivityProperty.USER_URN.bind(activity.getUserUrn())
        );
    }
}
