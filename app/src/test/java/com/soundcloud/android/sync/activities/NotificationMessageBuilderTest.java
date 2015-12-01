package com.soundcloud.android.sync.activities;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
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
        final PropertySet comment = TestPropertySets.activityTrackComment();
        final String trackTitle = getTrackTitle(comment);
        final String userName = getUserName(comment);

        final NotificationMessage message = builder
                .setComments(singletonList(comment))
                .build();

        assertThat(message.title).isEqualTo("1 new comment");
        assertThat(message.message).isEqualTo("new comment on " + trackTitle + " from " + userName);
        assertThat(message.ticker).isEqualTo("1 new comment");
    }

    @Test
    public void shouldDisplayNotificationForLikeOnly() {
        final PropertySet like = TestPropertySets.activityTrackLike();
        final String trackTitle = getTrackTitle(like);
        final String userName = getUserName(like);

        final NotificationMessage message = builder
                .setLikes(singletonList(like))
                .build();

        assertThat(message.title).isEqualTo("1 new like");
        assertThat(message.message).isEqualTo(userName + " likes " + trackTitle);
        assertThat(message.ticker).isEqualTo("1 new like");
    }

    @Test
    public void shouldDisplayNotificationForRepostOnly() {
        final PropertySet repost = TestPropertySets.activityTrackRepost();
        final String trackTitle = getTrackTitle(repost);
        final String userName = getUserName(repost);

        final NotificationMessage message = builder
                .setReposts(singletonList(repost))
                .build();

        assertThat(message.title).isEqualTo("1 new repost");
        assertThat(message.message).isEqualTo(userName + " reposted " + trackTitle);
        assertThat(message.ticker).isEqualTo("1 new repost");
    }

    @Test
    public void shouldDisplayNotificationForNewFollowerOnly() {
        final PropertySet follower = TestPropertySets.activityUserFollow();
        final String userName = getUserName(follower);

        final NotificationMessage message = builder
                .setFollowers(singletonList(follower))
                .build();

        assertThat(message.title).isEqualTo("1 new follower");
        assertThat(message.message).isEqualTo(userName + " followed you");
        assertThat(message.ticker).isEqualTo("1 new follower");
    }

    @Test
    public void shouldDisplayNotificationFor3NewFollower() {
        final PropertySet activity1 = TestPropertySets.activityUserFollow()
                .put(ActivityProperty.USER_NAME, "user1");
        final PropertySet activity2 = TestPropertySets.activityUserFollow()
                .put(ActivityProperty.USER_NAME, "user2");
        final PropertySet activity3 = TestPropertySets.activityUserFollow()
                .put(ActivityProperty.USER_NAME, "user3");

        final String userName1 = getUserName(activity1);
        final String userName2 = getUserName(activity2);

        final NotificationMessage message = builder
                .setFollowers(asList(activity1, activity2, activity3))
                .build();

        assertThat(message.title).isEqualTo("3 new followers");
        assertThat(message.message).isEqualTo(userName1 + ", " + userName2 + " and 1 other followed you");
        assertThat(message.ticker).isEqualTo("3 new followers");
    }

    @Test
    public void shouldDisplayNotificationForMoreThan3NewFollower() {
        final PropertySet activity1 = TestPropertySets.activityUserFollow()
                .put(ActivityProperty.USER_NAME, "user1");
        final PropertySet activity2 = TestPropertySets.activityUserFollow()
                .put(ActivityProperty.USER_NAME, "user2");
        final PropertySet activity3 = TestPropertySets.activityUserFollow()
                .put(ActivityProperty.USER_NAME, "user3");
        final PropertySet activity4 = TestPropertySets.activityUserFollow()
                .put(ActivityProperty.USER_NAME, "user4");

        final String userName1 = getUserName(activity1);
        final String userName2 = getUserName(activity2);

        final NotificationMessage message = builder
                .setFollowers(asList(activity1, activity2, activity3, activity4))
                .build();

        assertThat(message.title).isEqualTo("4 new followers");
        assertThat(message.message).isEqualTo(userName1 + ", " + userName2 + " and 2 others followed you");
        assertThat(message.ticker).isEqualTo("4 new followers");
    }

    @Test
    public void shouldDisplayNotificationForMixedNotifications() {
        final PropertySet comment = TestPropertySets.activityTrackComment()
                .put(ActivityProperty.USER_NAME, "user1");
        final PropertySet like = TestPropertySets.activityTrackLike()
                .put(ActivityProperty.USER_NAME, "user2");
        final PropertySet repost = TestPropertySets.activityPlaylistRepost()
                .put(ActivityProperty.USER_NAME, "user3");
        final PropertySet newFollower = TestPropertySets.activityUserFollow()
                .put(ActivityProperty.USER_NAME, "user4");

        final String commentName = getUserName(comment);
        final String likeName = getUserName(like);

        final NotificationMessage message = builder
                .setComments(singletonList(comment))
                .setLikes(singletonList(like))
                .setReposts(singletonList(repost))
                .setFollowers(singletonList(newFollower))
                .build();

        assertThat(message.title).isEqualTo("4 new activities");
        assertThat(message.message).isEqualTo("Comments and likes from " + commentName + ", " + likeName + " and others");
        assertThat(message.ticker).isEqualTo("4 new activities");
    }

    @Test
    public void shouldDedupeUsernames() {
        // 2 activities from same user
        final PropertySet comment = TestPropertySets.activityTrackComment()
                .put(ActivityProperty.USER_NAME, "user1");
        final PropertySet like = TestPropertySets.activityTrackLike()
                .put(ActivityProperty.USER_NAME, "user1");


        final NotificationMessage message = builder
                .setComments(singletonList(comment))
                .setLikes(singletonList(like))
                .build();

        assertThat(message.title).isEqualTo("2 new activities");
        assertThat(message.message).isEqualTo("from user1 on sounds of ze forzz");
        assertThat(message.ticker).isEqualTo("2 new activities");
    }

    @Test
    public void shouldDedupePlayableTitles() {
        // 2 activities from same user
        final PropertySet comment1 = TestPropertySets.activityTrackComment()
                .put(ActivityProperty.USER_NAME, "user1");
        final PropertySet comment2 = TestPropertySets.activityTrackComment()
                .put(ActivityProperty.USER_NAME, "user2");


        final NotificationMessage message = builder
                .setComments(asList(comment1, comment2))
                .build();

        assertThat(message.title).isEqualTo("2 new comments");
        assertThat(message.message).isEqualTo("2 new comments on sounds of ze forzz from user1 and user2");
        assertThat(message.ticker).isEqualTo("2 new comments");
    }

    private String getUserName(PropertySet activity) {
        return activity.get(ActivityProperty.USER_NAME);
    }

    private String getTrackTitle(PropertySet comment) {
        return comment.get(ActivityProperty.PLAYABLE_TITLE);
    }
}
