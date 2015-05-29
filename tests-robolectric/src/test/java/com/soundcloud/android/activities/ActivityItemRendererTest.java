package com.soundcloud.android.activities;

import com.soundcloud.android.Expect;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ActivityItemRendererTest {
    @Mock private LayoutInflater inflater;
    @Mock private ImageOperations imageOperations;
    private View itemView;

    private ActivityItemRenderer renderer;

    @Before
    public void setUp() throws Exception {
        final Context context = Robolectric.application;
        itemView = LayoutInflater.from(context).inflate(R.layout.engagement_list_item, new FrameLayout(context), false);
        renderer = new ActivityItemRenderer(context.getResources(), imageOperations);
    }

    @Test
    public void shouldBindAffiliationActivity() {
        final Date oneHourAgo = new Date(System.currentTimeMillis() - 60 * 60 * 1000);
        final List<PropertySet> propertySets = Arrays.asList(PropertySet.from(
                ActivityProperty.TYPE.bind(ActivityProperty.TYPE_FOLLOWER),
                ActivityProperty.USER_URN.bind(Urn.forUser(123L)),
                ActivityProperty.USER_NAME.bind("follower"),
                ActivityProperty.DATE.bind(oneHourAgo)
        ));
        renderer.bindItemView(0, itemView, propertySets);

        expect(R.id.username, "follower");
        expect(R.id.body, "started following you");
        expect(R.id.date, "1 hour ago");
    }

    @Test
    public void shouldBindLikeActivity() {
        final Date fiftyTwoMinutesAgo = new Date(System.currentTimeMillis() - 52 * 60 * 1000);
        final List<PropertySet> propertySets = Arrays.asList(PropertySet.from(
                ActivityProperty.TYPE.bind(ActivityProperty.TYPE_LIKE),
                ActivityProperty.USER_NAME.bind("User name"),
                ActivityProperty.USER_URN.bind(Urn.forUser(123L)),
                ActivityProperty.DATE.bind(fiftyTwoMinutesAgo),
                ActivityProperty.SOUND_TITLE.bind("Sound title")
        ));
        renderer.bindItemView(0, itemView, propertySets);

        expect(R.id.username, "User name");
        expect(R.id.body, "liked Sound title");
        expect(R.id.date, "52 minutes ago");
    }

    @Test
    public void shouldBindRepostActivity() {
        final Date fiftyTwoMinutesAgo = new Date(System.currentTimeMillis() - 52 * 60 * 1000);
        final List<PropertySet> propertySets = Arrays.asList(PropertySet.from(
                ActivityProperty.TYPE.bind(ActivityProperty.TYPE_REPOST),
                ActivityProperty.USER_NAME.bind("User name"),
                ActivityProperty.USER_URN.bind(Urn.forUser(123L)),
                ActivityProperty.DATE.bind(fiftyTwoMinutesAgo),
                ActivityProperty.SOUND_TITLE.bind("Sound title")
        ));
        renderer.bindItemView(0, itemView, propertySets);

        expect(R.id.username, "User name");
        expect(R.id.body, "reposted Sound title");
        expect(R.id.date, "52 minutes ago");

    }

    @Test
    public void shouldBindCommentActivity() {
        final Date fiftyTwoMinutesAgo = new Date(System.currentTimeMillis() - 52 * 60 * 1000);
        final List<PropertySet> propertySets = Arrays.asList(PropertySet.from(
                ActivityProperty.TYPE.bind(ActivityProperty.TYPE_COMMENT),
                ActivityProperty.USER_URN.bind(Urn.forUser(123L)),
                ActivityProperty.USER_NAME.bind("User name"),
                ActivityProperty.DATE.bind(fiftyTwoMinutesAgo),
                ActivityProperty.SOUND_TITLE.bind("Sound title")
        ));
        renderer.bindItemView(0, itemView, propertySets);

        expect(R.id.username, "User name");
        expect(R.id.body, "commented on Sound title");
        expect(R.id.date, "52 minutes ago");
    }


    private boolean expect(int id, String expected) {
        return Expect.expect(((TextView) itemView.findViewById(id)).getText()).toEqual(expected);
    }

}