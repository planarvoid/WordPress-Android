package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import org.junit.Test;

import java.util.List;

/**
 * Tests for notification messaging.
 */
public class SyncAdapterServiceNotificationTest extends SyncAdapterServiceTestBase {

    @Test
    public void shouldNotifyIfSyncedBefore() throws Exception {
        addCannedActivities("empty_collection.json", "e1_activities_1_oldest.json");
        SyncOutcome result = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);

        expect(result.getInfo().getContentText().toString()).toEqual(
                "Comments and likes from Liraz Axelrad, UnoFuego and others");
        expect(result.getInfo().getContentTitle().toString()).toEqual(
                "7 new activities");
        expect(result.getIntent().getAction()).toEqual(Actions.ACTIVITY);
    }

    @Test
    public void shouldNotRepeatNotification() throws Exception {
        addCannedActivities("e1_stream_1_oldest.json", "e1_activities_1_oldest.json");
        SyncOutcome result = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);

        expect(result.notifications.size()).toEqual(1);

        addCannedActivities("empty_collection.json", "empty_collection.json");
        result = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);
        expect(result.notifications).toBeEmpty();
    }

    // --------------- likes ---------------
    @Test
    public void shouldNotifyAboutActivityLikeOne() throws Exception {
        assertNotification("own_one_like.json",
                "New like",
                "A new like",
                "Paul Ko likes P. Watzlawick - Anleitung zum Ungl\u00fccklichsein");
    }

    @Test
    public void shouldNotifyAboutActivityLikeTwo() throws Exception {
        assertNotification("own_two_likes.json",
                "2 new likes",
                "2 new likes",
                "on P. Watzlawick - Anleitung zum Ungl\u00fccklichsein");
    }

    @Test
    public void shouldNotifyAboutActivityLikeMultiple() throws Exception {
        assertNotification("own_multi_likes.json",
                "3 new likes",
                "3 new likes",
                "on P. Watzlawick - Anleitung zum Ungl\u00fccklichsein, William Gibson & Cory Doctorow on 'Zero History'" +
                        " and other sounds");
    }

    // --------------- reposts ---------------
    @Test
    public void shouldNotifyAboutActivityRepostOne() throws Exception {
        assertNotification("own_one_repost.json",
                "New repost",
                "A new repost",
                "jberkel_testing reposted Ole L. - Chant Of The Raw Material");
    }

    @Test
    public void shouldNotifyAboutActivityRepostTwo() throws Exception {
        assertNotification("own_two_reposts.json",
                "2 new reposts",
                "2 new reposts",
                "on Ole L. - Chant Of The Raw Material");
    }

    @Test
    public void shouldNotifyAboutActivityRepostTwoDifferentSounds() throws Exception {
        assertNotification("own_multi_reposts.json",
                "3 new reposts",
                "3 new reposts",
                "on Momus sings Ashes To Ashes at Bei Roy and Ole L. - Chant Of The Raw Material");
    }

    @Test
    public void shouldNotifyAboutActivityRepostThreeDifferentSounds() throws Exception {
        assertNotification("own_multi_reposts_2.json",
                "4 new reposts",
                "4 new reposts",
                "on Nobody home, Momus sings Ashes To Ashes at Bei Roy and other sounds");
    }


    // --------------- comments ---------------
    @Test
    public void shouldNotifyAboutActivityCommentingSingle() throws Exception {
        assertNotification("own_one_comment.json",
                "1 new comment",
                "1 new comment",
                "new comment on Autotune at MTV from fronx");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingTwo() throws Exception {
        assertNotification("own_two_comments.json",
                "2 new comments",
                "2 new comments",
                "2 new comments on Autotune at MTV from fronx and bronx");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingThree() throws Exception {
        assertNotification("own_three_comments.json",
                "3 new comments",
                "3 new comments",
                "3 new comments on Autotune at MTV from fronx, bronx and others");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingFour() throws Exception {
        assertNotification("own_four_comments_different_tracks.json",
                "4 new comments",
                "4 new comments",
                "Comments from fronx, bronx and others");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingLikeTwoUsers() throws Exception {
        assertNotification("own_comment_like_same_track.json",
                "2 new activities",
                "2 new activities",
                "from fronx on Autotune at MTV");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingAndLikeTwoDifferentUsers() throws Exception {
        assertNotification("own_comment_like_different_tracks_two_users.json",
                "5 new activities",
                "5 new activities",
                "Comments and likes from bronx and fronx");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingAndLikeMultipleDifferentUsers() throws Exception {
        assertNotification("own_comment_like_different_tracks.json",
                "5 new activities",
                "5 new activities",
                "Comments and likes from Paul Ko, changmangoo and others");
    }

    @Test
    public void shouldUseCachedActivitiesToUpdateNotificationsWhenUserHasSeen() throws Exception {
        addCannedActivities("empty_collection.json", "e1_activities_1_oldest.json");
        SyncOutcome first = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);

        expect(first.getTicker()).toEqual("7 new activities");
        expect(first.getInfo().getContentTitle().toString()).toEqual("7 new activities");
        expect(first.getInfo().getContentText().toString()).toEqual("Comments and likes from Liraz Axelrad, UnoFuego and others");

        // user has already seen some stuff
        ContentStats.setLastSeen(DefaultTestRunner.application, Content.ME_ACTIVITIES,
                PublicApiWrapper.CloudDateFormat.fromString("2011/07/23 11:51:29 +0000").getTime());

        addCannedActivities("empty_collection.json", "e1_activities_2.json");
        SyncOutcome second = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);

        expect(second.getTicker()).toEqual("9 new activities");
        expect(second.getInfo().getContentTitle().toString()).toEqual("9 new activities");
        expect(second.getInfo().getContentText().toString()).toEqual("Comments and likes from D∃SIGNATED∀VΞ, Liraz Axelrad and others");
    }

    @Test
    public void shouldUseCachedActivitiesToUpdateNotifications() throws Exception {
        addCannedActivities("empty_collection.json",  "e1_activities_1_oldest.json");
        SyncOutcome first = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);

        expect(first.getTicker()).toEqual("7 new activities");
        expect(first.getInfo().getContentTitle().toString()).toEqual("7 new activities");
        expect(first.getInfo().getContentText().toString()).toEqual("Comments and likes from Liraz Axelrad, UnoFuego and others");

        addCannedActivities("empty_collection.json", "e1_activities_2.json");
        SyncOutcome second = doPerformSyncWithValidToken(DefaultTestRunner.application, false, null);

        expect(second.getTicker()).toEqual("9 new activities");
        expect(second.getInfo().getContentTitle().toString()).toEqual("9 new activities");
        expect(second.getInfo().getContentText().toString()).toEqual("Comments and likes from D∃SIGNATED∀VΞ, Liraz Axelrad and others");
    }

    private void assertNotification(String resource, String ticker, String title, String content) throws Exception {
        addCannedActivities(
                "empty_collection.json",
                resource
        );
        SoundCloudApplication app = DefaultTestRunner.application;

        ContentStats.setLastSeen(app, Content.ME_SOUND_STREAM, 1);
        ContentStats.setLastSeen(app, Content.ME_ACTIVITIES, 1);
        List<NotificationInfo> notifications = doPerformSyncWithValidToken(app, false, null).notifications;
        expect(notifications.size()).toEqual(1);
        NotificationInfo n = notifications.get(0);
        expect(n.n.tickerText.toString()).toEqual(ticker);
        expect(n.info.getContentTitle().toString()).toEqual(title);
        expect(n.info.getContentText().toString()).toEqual(content);
    }
}
