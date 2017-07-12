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
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class ActivitiesStorageTest extends StorageIntegrationTest {

    private static final long TIMESTAMP = 1000L;

    private ActivitiesStorage storage;

    private ApiTrackCommentActivity oldestActivity;
    private ApiTrackLikeActivity olderActivity;
    private ApiUserFollowActivity newestActivity;

    @Before
    public void setUp() throws Exception {
        storage = new ActivitiesStorage(propeller(), propellerRxV2());
        oldestActivity = apiTrackCommentActivity(new Date(TIMESTAMP));
        olderActivity = apiTrackLikeActivity(new Date(TIMESTAMP + 1));
        newestActivity = apiUserFollowActivity(new Date(TIMESTAMP + 2));
        testFixtures().insertTrackCommentActivity(oldestActivity);
        testFixtures().insertTrackLikeActivity(olderActivity);
        testFixtures().insertUserFollowActivity(newestActivity);
    }

    @Test
    public void shouldLoadLatestActivitiesInReverseChronologicalOrder() {
        final io.reactivex.observers.TestObserver<ActivityItem> subscriber = storage.timelineItems(Integer.MAX_VALUE).test();

        subscriber.assertValues(
                expectedPropertiesFor(newestActivity),
                expectedPropertiesFor(olderActivity),
                expectedPropertiesFor(oldestActivity)
        );
        subscriber.assertComplete();
    }

    @Test
    public void shouldNotReturnUnsupportedActivitiesFromLatestActivities() {
        testFixtures().insertUnsupportedActivity();

        final io.reactivex.observers.TestObserver<ActivityItem> subscriber = storage.timelineItems(Integer.MAX_VALUE).test();

        subscriber.assertValues(
                expectedPropertiesFor(newestActivity),
                expectedPropertiesFor(olderActivity),
                expectedPropertiesFor(oldestActivity)
        );
        subscriber.assertComplete();
    }

    @Test
    public void shouldLimitLatestActivitiesResultSet() {
        final int limit = 1;
        final io.reactivex.observers.TestObserver<ActivityItem> subscriber = storage.timelineItems(limit).test();

        subscriber.assertValue(expectedPropertiesFor(newestActivity));
        subscriber.assertComplete();
    }

    @Test
    public void shouldLoadActivitiesBeforeGivenTimestampInReverseChronologicalOrder() {
        final io.reactivex.observers.TestObserver<ActivityItem> subscriber = storage.timelineItemsBefore(TIMESTAMP + 2, Integer.MAX_VALUE).test();

        subscriber.assertValues(
                expectedPropertiesFor(olderActivity),
                expectedPropertiesFor(oldestActivity)
        );
        subscriber.assertComplete();
    }

    @Test
    public void shouldNotReturnUnsupportedActivitiesFromActivitiesBeforeGivenTimestamp() {
        testFixtures().insertUnsupportedActivity();

        final io.reactivex.observers.TestObserver<ActivityItem> subscriber = storage.timelineItemsBefore(Long.MAX_VALUE, Integer.MAX_VALUE).test();

        subscriber.assertValues(
                expectedPropertiesFor(newestActivity),
                expectedPropertiesFor(olderActivity),
                expectedPropertiesFor(oldestActivity)
        );
        subscriber.assertComplete();
    }

    @Test
    public void shouldLimitActivitiesBeforeGivenTimestamp() {
        final int limit = 1;
        final io.reactivex.observers.TestObserver<ActivityItem> subscriber = storage.timelineItemsBefore(TIMESTAMP + 2, limit).test();

        subscriber.assertValue(expectedPropertiesFor(olderActivity));
        subscriber.assertComplete();
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
    @Issue(ref = "https://github.com/soundcloud/android-listeners/issues/4673")
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
    @Issue(ref = "https://github.com/soundcloud/android-listeners/issues/4673")
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
        final TestObserver<Integer> observer = storage.timelineItemCountSince(TIMESTAMP).test();

        observer.assertValue(2);
    }

    private ActivityItem expectedPropertiesFor(ApiUserFollowActivity activity) {
        return ActivityItem.create(
                activity.getCreatedAt(),
                ActivityKind.USER_FOLLOW,
                activity.getUser().getUsername(),
                Strings.EMPTY,
                Optional.absent(),
                activity.getUserUrn(),
                activity.getUser().getImageUrlTemplate()
        );
    }

    private ActivityItem expectedPropertiesFor(ApiTrackLikeActivity activity) {
        return ActivityItem.create(
                activity.getCreatedAt(),
                ActivityKind.TRACK_LIKE,
                activity.getUser().getUsername(),
                activity.getTrack().getTitle(),
                Optional.absent(),
                activity.getUserUrn(),
                activity.getUser().getImageUrlTemplate()
        );
    }

    private ActivityItem expectedPropertiesFor(ApiTrackCommentActivity activity) {
        return ActivityItem.create(
                activity.getCreatedAt(),
                ActivityKind.TRACK_COMMENT,
                activity.getUser().getUsername(),
                activity.getTrack().getTitle(),
                Optional.of(activity.getTrack().getUrn()),
                activity.getUserUrn(),
                activity.getUser().getImageUrlTemplate()
        );
    }
}
