package com.soundcloud.android.activities;

import static java.util.Collections.singletonList;
import static org.assertj.android.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Date;

public class ActivityItemRendererTest extends AndroidUnitTest {
    @Mock private LayoutInflater inflater;
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
        final ActivityItem activityItem = new ActivityItem(PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.USER_FOLLOW),
                ActivityProperty.USER_URN.bind(Urn.forUser(123L)),
                ActivityProperty.USER_NAME.bind("follower"),
                ActivityProperty.DATE.bind(oneHourAgo)
        ));
        renderer.bindItemView(0, itemView, singletonList(activityItem));

        assertText(R.id.username, "follower");
        assertText(R.id.body, "started following you");
        assertText(R.id.date, "1 hour ago");
    }

    @Test
    public void shouldBindLikeActivity() {
        final Date fiftyTwoMinutesAgo = new Date(System.currentTimeMillis() - 52 * 60 * 1000);
        final ActivityItem activityItem = new ActivityItem(PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.TRACK_LIKE),
                ActivityProperty.USER_NAME.bind("User name"),
                ActivityProperty.USER_URN.bind(Urn.forUser(123L)),
                ActivityProperty.DATE.bind(fiftyTwoMinutesAgo),
                ActivityProperty.PLAYABLE_TITLE.bind("Sound title")
        ));
        renderer.bindItemView(0, itemView, singletonList(activityItem));

        assertText(R.id.username, "User name");
        assertText(R.id.body, "liked Sound title");
        assertText(R.id.date, "52 minutes ago");
    }

    @Test
    public void shouldBindRepostActivity() {
        final Date fiftyTwoMinutesAgo = new Date(System.currentTimeMillis() - 52 * 60 * 1000);
        final ActivityItem activityItem = new ActivityItem(PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.TRACK_REPOST),
                ActivityProperty.USER_NAME.bind("User name"),
                ActivityProperty.USER_URN.bind(Urn.forUser(123L)),
                ActivityProperty.DATE.bind(fiftyTwoMinutesAgo),
                ActivityProperty.PLAYABLE_TITLE.bind("Sound title")
        ));
        renderer.bindItemView(0, itemView, singletonList(activityItem));

        assertText(R.id.username, "User name");
        assertText(R.id.body, "reposted Sound title");
        assertText(R.id.date, "52 minutes ago");
    }

    @Test
    public void shouldBindCommentActivity() {
        final Date fiftyTwoMinutesAgo = new Date(System.currentTimeMillis() - 52 * 60 * 1000);
        final ActivityItem activityItem = new ActivityItem(PropertySet.from(
                ActivityProperty.KIND.bind(ActivityKind.TRACK_COMMENT),
                ActivityProperty.USER_URN.bind(Urn.forUser(123L)),
                ActivityProperty.USER_NAME.bind("User name"),
                ActivityProperty.DATE.bind(fiftyTwoMinutesAgo),
                ActivityProperty.PLAYABLE_TITLE.bind("Sound title")
        ));
        renderer.bindItemView(0, itemView, singletonList(activityItem));

        assertText(R.id.username, "User name");
        assertText(R.id.body, "commented on Sound title");
        assertText(R.id.date, "52 minutes ago");
    }

    private void assertText(int id, String expected) {
        assertThat((TextView) itemView.findViewById(id)).hasText(expected);
    }
}
