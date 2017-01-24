package com.soundcloud.android.activities;

import static java.util.Collections.singletonList;
import static org.assertj.android.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Date;

public class ActivityItemRendererTest extends AndroidUnitTest {
    @Mock private ImageOperations imageOperations;
    private View itemView;

    private ActivityItemRenderer renderer;

    @Before
    public void setUp() throws Exception {
        itemView = LayoutInflater.from(context()).inflate(
                R.layout.engagement_list_item, new FrameLayout(context()), false);
        renderer = new ActivityItemRenderer(resources(), imageOperations);
    }

    @Test
    public void shouldBindFollowActivity() {
        final Date oneHourAgo = new Date(System.currentTimeMillis() - 60 * 60 * 1000);
        final ActivityItem activityItem = ActivityItem.create(
                oneHourAgo,
                ActivityKind.USER_FOLLOW,
                "follower",
                Strings.EMPTY,
                Optional.absent(),
                Urn.forUser(123L)
        );

        renderer.bindItemView(0, itemView, singletonList(activityItem));

        assertText(R.id.username, "follower");
        assertText(R.id.body, "started following you");
        assertText(R.id.date, "1 hour ago");
    }

    @Test
    public void shouldBindLikeActivity() {
        final Date fiftyTwoMinutesAgo = new Date(System.currentTimeMillis() - 52 * 60 * 1000);
        final ActivityItem activityItem = ActivityItem.create(
                fiftyTwoMinutesAgo,
                ActivityKind.TRACK_LIKE,
                "User name",
                "Sound title",
                Optional.absent(),
                Urn.forUser(123L)
        );
        renderer.bindItemView(0, itemView, singletonList(activityItem));

        assertText(R.id.username, "User name");
        assertText(R.id.body, "liked Sound title");
        assertText(R.id.date, "52 minutes ago");
    }

    @Test
    public void shouldBindRepostActivity() {
        final Date fiftyTwoMinutesAgo = new Date(System.currentTimeMillis() - 52 * 60 * 1000);
        final ActivityItem activityItem = ActivityItem.create(
                fiftyTwoMinutesAgo,
                ActivityKind.TRACK_REPOST,
                "User name",
                "Sound title",
                Optional.absent(),
                Urn.forUser(123L)
        );
        renderer.bindItemView(0, itemView, singletonList(activityItem));

        assertText(R.id.username, "User name");
        assertText(R.id.body, "reposted Sound title");
        assertText(R.id.date, "52 minutes ago");
    }

    @Test
    public void shouldBindCommentActivity() {
        final Date fiftyTwoMinutesAgo = new Date(System.currentTimeMillis() - 52 * 60 * 1000);
        final ActivityItem activityItem = ActivityItem.create(
                fiftyTwoMinutesAgo,
                ActivityKind.TRACK_COMMENT,
                "User name",
                "Sound title",
                Optional.absent(),
                Urn.forUser(123L)
        );
        renderer.bindItemView(0, itemView, singletonList(activityItem));

        assertText(R.id.username, "User name");
        assertText(R.id.body, "commented on Sound title");
        assertText(R.id.date, "52 minutes ago");
    }

    private void assertText(int id, String expected) {
        assertThat((TextView) itemView.findViewById(id)).hasText(expected);
    }
}
