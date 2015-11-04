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

    @Before
    public void setUp() throws Exception {
        storage = new ActivitiesStorage(propellerRx());
    }

    @Test
    public void shouldIncludeLimitedTrackLikesInInitialActivityItems() {
        ApiTrackLikeActivity activity1 = apiTrackLikeActivity(new Date(TIMESTAMP));
        ApiTrackLikeActivity activity2 = apiTrackLikeActivity(new Date(TIMESTAMP + 1));
        testFixtures().insertTrackLikeActivity(activity1);
        testFixtures().insertTrackLikeActivity(activity2);

        storage.initialActivityItems(1).subscribe(subscriber);

        subscriber.assertValue(expectedTrackLikePropertiesFor(activity2));
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
