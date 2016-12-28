package com.soundcloud.android.profile;

import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

import java.util.Date;
import java.util.List;

public class PostsStorageTest extends StorageIntegrationTest {

    private static final Date POSTED_DATE_1 = new Date(100000);
    private static final Date POSTED_DATE_2 = new Date(200000);

    private PostsStorage storage;
    private ApiUser user;
    private PropertySet post1;
    private PropertySet post2;

    final TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();

    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() {
        user = testFixtures().insertUser();

        storage = new PostsStorage(propellerRx());

        when(accountOperations.getLoggedInUserUrn()).thenReturn(user.getUrn());
    }

    @Test
    public void shouldLoadLastPublicPostedTrackWithDatePostedAndPermalink() throws Exception {
        post1 = createTrackPostForLastPostedAt(POSTED_DATE_2);
        createTrackPostForLastPostedAt(POSTED_DATE_1);
        TestSubscriber<PropertySet> subscriber = new TestSubscriber<>();

        storage.loadLastPublicPostedTrack().subscribe(subscriber);

        subscriber.assertValue(post1);
    }

    @Test
    public void shouldLoadLastPublicPostedTrackExcludingPrivateTracks() throws Exception {
        createPrivateTrackPostForLastPostedAt(POSTED_DATE_2);
        post2 = createTrackPostForLastPostedAt(POSTED_DATE_1);
        TestSubscriber<PropertySet> subscriber = new TestSubscriber<>();

        storage.loadLastPublicPostedTrack().subscribe(subscriber);

        subscriber.assertValue(post2);
    }

    private ApiTrack createTrackAt(Date creationDate) {
        return testFixtures().insertTrackWithCreationDate(user, creationDate);
    }

    private void createTrackPostWithId(long trackId, Date postedAt) {
        testFixtures().insertTrackPost(trackId, postedAt.getTime(), false);
    }

    private PropertySet createTrackPostForLastPostedAt(Date postedAt) {
        ApiTrack track = createTrackAt(postedAt);
        createTrackPostWithId(track.getUrn().getNumericId(), postedAt);
        return createTrackPostForLastPostedPropertySet(track);
    }

    private PropertySet createTrackPostForLastPostedPropertySet(ApiTrack track) {
        return TrackItem.from(track).slice(
                TrackProperty.URN,
                TrackProperty.PERMALINK_URL
        ).put(PostProperty.CREATED_AT, track.getCreatedAt());
    }

    private ApiTrack createPrivateTrackAt(Date creationDate) {
        return testFixtures().insertPrivateTrackWithCreationDate(user, creationDate);
    }

    private PropertySet createPrivateTrackPostForLastPostedAt(Date postedAt) {
        ApiTrack track = createPrivateTrackAt(postedAt);
        createTrackPostWithId(track.getUrn().getNumericId(), postedAt);
        return createTrackPostForLastPostedPropertySet(track);
    }

}
