package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.Actions;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;

import java.util.List;

/**
 * Tests for notification messaging.
 */
public class SyncAdapterServiceNotificationTest extends SyncAdapterServiceTestBase {
    @Test
    public void testIncomingNotificationMessage() throws Exception {
        Activities activities = SoundCloudApplication.MODEL_MANAGER.getActivitiesFromJson(getClass().getResourceAsStream("e1_stream.json"));
        String message = Message.getIncomingNotificationMessage(
                DefaultTestRunner.application, activities);

        expect(message).toEqual("from WADR, T-E-E-D and others");
    }

    @Test
    public void shouldNotifyIfSyncedBefore() throws Exception {
        addCannedActivities("e1_stream_1_oldest.json", "empty_events.json");
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, null);

        expect(result.getInfo().getContentText().toString()).toEqual(
                "from Bad Panda Records, The Takeaway and others");
        expect(result.getInfo().getContentTitle().toString()).toEqual(
                "20 new sounds");
        expect(result.getIntent().getAction()).toEqual(Actions.STREAM);
    }

    @Test
    public void shouldNotRepeatNotification() throws Exception {
        addCannedActivities("e1_stream_1_oldest.json", "e1_activities_1_oldest.json");
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, null);

        expect(result.notifications.size()).toEqual(2);

        addCannedActivities("empty_events.json", "empty_events.json");
        result = doPerformSync(DefaultTestRunner.application, false, null);
        expect(result.notifications).toBeEmpty();
    }

    @Test
    public void shouldNotifyAboutIncoming() throws Exception {
        addCannedActivities("e1_stream_1_oldest.json", "empty_events.json");

        SoundCloudApplication app = DefaultTestRunner.application;
        List<NotificationInfo> notifications = doPerformSync(app, false, null).notifications;

        expect(notifications.size()).toEqual(1);
        NotificationInfo n = notifications.get(0);
        expect(n.info.getContentTitle().toString())
                .toEqual("20 new sounds");

        expect(n.info.getContentText().toString())
                .toEqual("from Bad Panda Records, The Takeaway and others");

        expect(n.getIntent().getAction()).toEqual(Actions.STREAM);
    }

    @Test
    public void shouldSendTwoSeparateNotifications() throws Exception {
        addCannedActivities("e1_stream_1_oldest.json", "e1_activities_2.json");

        List<NotificationInfo> notifications = doPerformSync(DefaultTestRunner.application, false, null).notifications;
        expect(notifications.size()).toEqual(2);

        expect(notifications.get(0).info.getContentTitle().toString())
                .toEqual("20 new sounds");
        expect(notifications.get(0).info.getContentText().toString())
                .toEqual("from Bad Panda Records, The Takeaway and others");

        expect(notifications.get(0).getIntent().getAction())
                .toEqual(Actions.STREAM);

        expect(notifications.get(1).info.getContentTitle().toString())
                .toEqual("2 new activities");
        expect(notifications.get(1).info.getContentText().toString())
                .toEqual("Comments and likes from D∃SIGNATED∀VΞ and Liraz Axelrad");

        expect(notifications.get(1).getIntent().getAction())
                .toEqual(Actions.ACTIVITY);
    }

    @Test
    public void shouldNotifyAboutActivityFavoritingOne() throws Exception {
        assertNotification("own_one_favoriting.json",
                "New like",
                "A new like",
                "Paul Ko likes P. Watzlawick - Anleitung zum Ungl\u00fccklichsein");
    }

    @Test
    public void shouldNotifyAboutActivityFavoritingTwo() throws Exception {
        assertNotification("own_two_favoritings.json",
                "2 new likes",
                "2 new likes",
                "on P. Watzlawick - Anleitung zum Ungl\u00fccklichsein");
    }

    @Test
    public void shouldNotifyAboutActivityFavoritingMultiple() throws Exception {
        assertNotification("own_multi_favoritings.json",
                "3 new likes",
                "3 new likes",
                "on P. Watzlawick - Anleitung zum Ungl\u00fccklichsein, William Gibson & Cory Doctorow on 'Zero History'" +
                        " and other sounds");
    }

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
    public void shouldNotifyAboutActivityCommentingAndFavoritingTwoUsers() throws Exception {
        assertNotification("own_comment_favoriting_same_track.json",
                "2 new activities",
                "2 new activities",
                "from fronx on Autotune at MTV");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingAndFavoritingTwoDifferentUsers() throws Exception {
        assertNotification("own_comment_favoriting_different_tracks_two_users.json",
                "5 new activities",
                "5 new activities",
                "Comments and likes from bronx and fronx");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingAndFavoritingMultipleDifferentUsers() throws Exception {
        assertNotification("own_comment_favoriting_different_tracks.json",
                "5 new activities",
                "5 new activities",
                "Comments and likes from Paul Ko, changmangoo and others");
    }

    @Test
    public void shouldNotify99PlusItems() throws Exception {
        addCannedActivities(
                "e1_stream.json",
                "e1_stream_2.json",
                "e1_stream_oldest.json",
                "empty_events.json");

        List<NotificationInfo> notifications = doPerformSync(DefaultTestRunner.application, false, null).notifications;
        expect(notifications.size()).toEqual(1);
        NotificationInfo n = notifications.get(0);
        expect(n.info.getContentTitle().toString())
                .toEqual("99+ new sounds");
    }

    @Test
    public void shouldUseCachedActivitiesToUpdateNotificationsWhenUserHasSeen() throws Exception {
        addCannedActivities("empty_events.json", "e1_activities_1_oldest.json");
        SyncOutcome first = doPerformSync(DefaultTestRunner.application, false, null);

        expect(first.getTicker()).toEqual("7 new activities");
        expect(first.getInfo().getContentTitle().toString()).toEqual("7 new activities");
        expect(first.getInfo().getContentText().toString()).toEqual("Comments and likes from Liraz Axelrad, UnoFuego and others");

        // user has already seen some stuff
        ContentStats.setLastSeen(DefaultTestRunner.application, Content.ME_ACTIVITIES,
                AndroidCloudAPI.CloudDateFormat.fromString("2011/07/23 11:51:29 +0000").getTime());

        addCannedActivities("empty_events.json", "e1_activities_2.json");
        SyncOutcome second = doPerformSync(DefaultTestRunner.application, false, null);

        expect(second.getTicker()).toEqual("9 new activities");
        expect(second.getInfo().getContentTitle().toString()).toEqual("9 new activities");
        expect(second.getInfo().getContentText().toString()).toEqual("Comments and likes from D∃SIGNATED∀VΞ, Liraz Axelrad and others");
    }

    @Test
    public void shouldUseCachedActivitiesToUpdateNotifications() throws Exception {
        addCannedActivities("empty_events.json",  "e1_activities_1_oldest.json");
        SyncOutcome first = doPerformSync(DefaultTestRunner.application, false, null);

        expect(first.getTicker()).toEqual("7 new activities");
        expect(first.getInfo().getContentTitle().toString()).toEqual("7 new activities");
        expect(first.getInfo().getContentText().toString()).toEqual("Comments and likes from Liraz Axelrad, UnoFuego and others");

        addCannedActivities("empty_events.json", "e1_activities_2.json");
        SyncOutcome second = doPerformSync(DefaultTestRunner.application, false, null);

        expect(second.getTicker()).toEqual("9 new activities");
        expect(second.getInfo().getContentTitle().toString()).toEqual("9 new activities");
        expect(second.getInfo().getContentText().toString()).toEqual("Comments and likes from D∃SIGNATED∀VΞ, Liraz Axelrad and others");
    }

    private void assertNotification(String resource, String ticker, String title, String content) throws Exception {
        addCannedActivities(
                "empty_events.json",
                resource
        );
        SoundCloudApplication app = DefaultTestRunner.application;

        ContentStats.setLastSeen(app, Content.ME_SOUND_STREAM, 1);
        ContentStats.setLastSeen(app, Content.ME_ACTIVITIES, 1);
        List<NotificationInfo> notifications = doPerformSync(app, false, null).notifications;
        expect(notifications.size()).toEqual(1);
        NotificationInfo n = notifications.get(0);
        expect(n.n.tickerText.toString()).toEqual(ticker);
        expect(n.info.getContentTitle().toString()).toEqual(title);
        expect(n.info.getContentText().toString()).toEqual(content);
    }
}
