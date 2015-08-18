package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

public class NotificationMessageBuilderTest extends AndroidUnitTest {
    private NotificationMessage.Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = new NotificationMessage.Builder(resources());
    }

    @Test
    public void shouldDisplayNotificationForCommentOnly() {
        final Activities comment = new Activities(ActivityFixtures.forComment());
        final String trackTitle = getTrackTitle(comment);
        final String userName = getUserName(comment);

        final NotificationMessage message = builder
                .setComments(comment)
                .build();

        assertThat(message.title).isEqualTo("1 new comment");
        assertThat(message.message).isEqualTo("new comment on " + trackTitle + " from " + userName);
        assertThat(message.ticker).isEqualTo("1 new comment");
    }

    @Test
    public void shouldDisplayNotificationForLikeOnly() {
        final Activities like = new Activities(ActivityFixtures.forLike());
        final String trackTitle = getTrackTitle(like);
        final String userName = getUserName(like);

        final NotificationMessage message = builder
                .setLikes(like)
                .build();

        assertThat(message.title).isEqualTo("1 new like");
        assertThat(message.message).isEqualTo(userName + " likes " + trackTitle);
        assertThat(message.ticker).isEqualTo("1 new like");
    }

    @Test
    public void shouldDisplayNotificationForRepostOnly() {
        final Activities repost = new Activities(ActivityFixtures.forRepost());
        final String trackTitle = getTrackTitle(repost);
        final String userName = getUserName(repost);

        final NotificationMessage message = builder
                .setReposts(repost)
                .build();

        assertThat(message.title).isEqualTo("1 new repost");
        assertThat(message.message).isEqualTo(userName + " reposted " + trackTitle);
        assertThat(message.ticker).isEqualTo("1 new repost");
    }

    @Test
    public void shouldDisplayNotificationForNewFollowerOnly() {
        final Activities follower = new Activities(ActivityFixtures.forNewFollower());
        final String userName = getUserName(follower);

        final NotificationMessage message = builder
                .setFollowers(follower)
                .build();

        assertThat(message.title).isEqualTo("1 new follower");
        assertThat(message.message).isEqualTo(userName + " followed you");
        assertThat(message.ticker).isEqualTo("1 new follower");
    }

    @Test
    public void shouldDisplayNotificationFor3NewFollower() {
        final Activity activity1 = ActivityFixtures.forNewFollower();
        final Activity activity2 = ActivityFixtures.forNewFollower();
        final Activity activity3 = ActivityFixtures.forNewFollower();
        final Activities follower = new Activities(activity1, activity2, activity3);

        final String userName1 = getUserName(activity1);
        final String userName2 = getUserName(activity2);

        final NotificationMessage message = builder
                .setFollowers(follower)
                .build();

        assertThat(message.title).isEqualTo("3 new followers");
        assertThat(message.message).isEqualTo(userName1 + ", " + userName2 + " and 1 other followed you");
        assertThat(message.ticker).isEqualTo("3 new followers");
    }

    @Test
    public void shouldDisplayNotificationForMoreThan3NewFollower() {
        final Activity activity1 = ActivityFixtures.forNewFollower();
        final Activity activity2 = ActivityFixtures.forNewFollower();
        final Activity activity3 = ActivityFixtures.forNewFollower();
        final Activity activity4 = ActivityFixtures.forNewFollower();
        final Activities follower = new Activities(activity1, activity2, activity3, activity4);

        final String userName1 = getUserName(activity1);
        final String userName2 = getUserName(activity2);

        final NotificationMessage message = builder
                .setFollowers(follower)
                .build();

        assertThat(message.title).isEqualTo("4 new followers");
        assertThat(message.message).isEqualTo(userName1 + ", " + userName2 + " and 2 others followed you");
        assertThat(message.ticker).isEqualTo("4 new followers");
    }

    @Test
    public void shouldDisplayNotificationForMixedNotifications() {
        final Activities comment = new Activities(ActivityFixtures.forComment());
        final Activities like = new Activities(ActivityFixtures.forLike());
        final Activities repost = new Activities(ActivityFixtures.forRepost());
        final Activities newFollower = new Activities(ActivityFixtures.forNewFollower());
        final Activities all = comment.merge(like).merge(repost).merge(newFollower);

        final String commentName = getUserName(comment);
        final String likeName = getUserName(like);

        final NotificationMessage message = builder
                .setComments(comment)
                .setLikes(like)
                .setReposts(repost)
                .setFollowers(newFollower)
                .setMixed(all)
                .build();

        assertThat(message.title).isEqualTo("4 new activities");
        assertThat(message.message).isEqualTo("Comments and likes from " + commentName + ", " + likeName + " and others");
        assertThat(message.ticker).isEqualTo("4 new activities");
    }


    private String getUserName(Activities like) {
        return like.getUniqueUsers().get(0).getDisplayName();
    }


    private String getUserName(Activity activity) {
        return activity.getUser().getDisplayName();
    }

    private String getTrackTitle(Activities comment) {
        return comment.getUniquePlayables().get(0).getTitle();
    }
}