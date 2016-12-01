package com.soundcloud.android.activities;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiTrackCommentActivity;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiTrackLikeActivity;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiUserFollowActivity;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.sync.activities.ApiPlaylistRepostActivity;
import com.soundcloud.android.sync.activities.ApiTrackCommentActivity;
import com.soundcloud.android.sync.activities.ApiTrackLikeActivity;
import com.soundcloud.android.sync.activities.ApiUserFollowActivity;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.annotations.Issue;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;

import java.util.Date;
import java.util.List;

public class ActivitiesStorageTest extends StorageIntegrationTest {

    private static final long TIMESTAMP = 1000L;

    private ActivitiesStorage storage;

    private TestSubscriber<ActivityItem> subscriber = new TestSubscriber<>();
    private ApiTrackCommentActivity oldestActivity;
    private ApiTrackLikeActivity olderActivity;
    private ApiUserFollowActivity newestActivity;

    @Before
    public void setUp() throws Exception {
        storage = new ActivitiesStorage(propeller(), propellerRx());
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

    @Test
    public void shouldLoadActivitiesAfterGivenTimestampInReverseChronologicalOrder() {
        List<ActivityItem> activities = storage.timelineItemsSince(TIMESTAMP, Integer.MAX_VALUE);

        assertThat(activities).containsExactly(
                expectedPropertiesFor(newestActivity),
                expectedPropertiesFor(olderActivity)
        );
    }

    @Test
    public void shouldLimitActivitiesSinceGivenTimestamp() {
        List<ActivityItem> activities = storage.timelineItemsSince(TIMESTAMP, 1);

        assertThat(activities).containsExactly(expectedPropertiesFor(newestActivity));
    }

    @Test
    public void shouldNotReturnUnsupportedActivitiesFromActivitiesSinceGivenTimestamp() {
        testFixtures().insertUnsupportedActivity();

        List<ActivityItem> activities = storage.timelineItemsSince(TIMESTAMP, Integer.MAX_VALUE);

        assertThat(activities).containsExactly(
                expectedPropertiesFor(newestActivity),
                expectedPropertiesFor(olderActivity)
        );
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/android/issues/4673")
    public void shouldNotReturnActivityPlaylistPendingRemovalSound() {
        ApiPlaylist playlist = testFixtures().insertPlaylistPendingRemoval();
        ApiPlaylistRepostActivity playlistRepostActivity = ModelFixtures.apiPlaylistRepostActivity(playlist);
        testFixtures().insertPlaylistRepostActivityWithoutPlaylist(playlistRepostActivity);

        List<ActivityItem> activities = storage.timelineItemsSince(TIMESTAMP, Integer.MAX_VALUE);
        assertThat(activities).containsExactly(
                expectedPropertiesFor(newestActivity),
                expectedPropertiesFor(olderActivity)
        );
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/android/issues/4673")
    public void shouldNotReturnActivityWithoutAssociatedSound() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        ApiPlaylistRepostActivity playlistRepostActivity = ModelFixtures.apiPlaylistRepostActivity(playlist);
        testFixtures().insertPlaylistRepostActivityWithoutPlaylist(playlistRepostActivity);

        List<ActivityItem> activities = storage.timelineItemsSince(TIMESTAMP, Integer.MAX_VALUE);
        assertThat(activities).containsExactly(
                expectedPropertiesFor(newestActivity),
                expectedPropertiesFor(olderActivity)
        );
    }

    @Test
    public void loadActivityItemsCountSinceOnlyCountsItemsNewerThanTheGivenTimestamp() {
        TestObserver<Integer> observer = new TestObserver<>();

        storage.timelineItemCountSince(TIMESTAMP).subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(2);
    }

    private ActivityItem expectedPropertiesFor(ApiUserFollowActivity activity) {
        return ActivityItem.fromPropertySet(PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.USER_FOLLOW),
                ActivityProperty.DATE.bind(activity.getCreatedAt()),
                ActivityProperty.USER_NAME.bind(activity.getUser().getUsername()),
                ActivityProperty.USER_URN.bind(activity.getUserUrn())
        ));
    }

    private ActivityItem expectedPropertiesFor(ApiTrackLikeActivity activity) {
        return ActivityItem.fromPropertySet(PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.TRACK_LIKE),
                ActivityProperty.DATE.bind(activity.getCreatedAt()),
                ActivityProperty.PLAYABLE_TITLE.bind(activity.getTrack().getTitle()),
                ActivityProperty.USER_NAME.bind(activity.getUser().getUsername()),
                ActivityProperty.USER_URN.bind(activity.getUserUrn())
        ));
    }

    private ActivityItem expectedPropertiesFor(ApiTrackCommentActivity activity) {
        return ActivityItem.fromPropertySet(PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.TRACK_COMMENT),
                ActivityProperty.DATE.bind(activity.getCreatedAt()),
                ActivityProperty.COMMENTED_TRACK_URN.bind(activity.getTrack().getUrn()),
                ActivityProperty.PLAYABLE_TITLE.bind(activity.getTrack().getTitle()),
                ActivityProperty.USER_NAME.bind(activity.getUser().getUsername()),
                ActivityProperty.USER_URN.bind(activity.getUserUrn())
        ));
    }
}
