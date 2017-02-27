package com.soundcloud.android.profile;

import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

import java.util.Date;

public class PostsStorageTest extends StorageIntegrationTest {

    private static final Date POSTED_DATE_1 = new Date(100000);
    private static final Date POSTED_DATE_2 = new Date(200000);

    private PostsStorage storage;
    private ApiUser user;
    private LastPostedTrack post1;
    private LastPostedTrack post2;

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
        TestSubscriber<LastPostedTrack> subscriber = new TestSubscriber<>();

        storage.loadLastPublicPostedTrack().subscribe(subscriber);

        subscriber.assertValue(post1);
    }

    @Test
    public void shouldLoadLastPublicPostedTrackExcludingPrivateTracks() throws Exception {
        createPrivateTrackPostForLastPostedAt(POSTED_DATE_2);
        post2 = createTrackPostForLastPostedAt(POSTED_DATE_1);
        TestSubscriber<LastPostedTrack> subscriber = new TestSubscriber<>();

        storage.loadLastPublicPostedTrack().subscribe(subscriber);

        subscriber.assertValue(post2);
    }

    private ApiTrack createTrackAt(Date creationDate) {
        return testFixtures().insertTrackWithCreationDate(user, creationDate);
    }

    private void createTrackPostWithId(long trackId, Date postedAt) {
        testFixtures().insertTrackPost(trackId, postedAt.getTime(), false);
    }

    private LastPostedTrack createTrackPostForLastPostedAt(Date postedAt) {
        ApiTrack track = createTrackAt(postedAt);
        createTrackPostWithId(track.getUrn().getNumericId(), postedAt);
        return createTrackPostForLastPostedApiTrack(track);
    }

    private LastPostedTrack createTrackPostForLastPostedApiTrack(ApiTrack track) {
        return LastPostedTrack.create(track.getUrn(), track.getCreatedAt(), track.getPermalinkUrl());
    }

    private ApiTrack createPrivateTrackAt(Date creationDate) {
        return testFixtures().insertPrivateTrackWithCreationDate(user, creationDate);
    }

    private LastPostedTrack createPrivateTrackPostForLastPostedAt(Date postedAt) {
        ApiTrack track = createPrivateTrackAt(postedAt);
        createTrackPostWithId(track.getUrn().getNumericId(), postedAt);
        return createTrackPostForLastPostedApiTrack(track);
    }

}
