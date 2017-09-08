package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.discovery.systemplaylist.SystemPlaylistActivity.EXTRA_FOR_NEW_FOR_YOU;
import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.discovery.SystemPlaylistScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import android.content.Intent;

public class NewForYouTest extends ActivityTest<SystemPlaylistActivity> {

    private static final Intent START_PARAM_INTENT = new Intent().putExtra(EXTRA_FOR_NEW_FOR_YOU, true);

    public NewForYouTest() {
        super(SystemPlaylistActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        mrLocalLocal.startEventTracking();

        setActivityIntent(START_PARAM_INTENT);
    }

    @Test
    public void testNewForYouPlayback() throws Exception {
        final SystemPlaylistScreen systemPlaylistScreen = new SystemPlaylistScreen(solo);

        assertThat(systemPlaylistScreen, is(visible()));
        assertThat("New for you screen title should be 'The Upload'",
                   systemPlaylistScreen.getActionBarTitle(),
                   equalTo("The Upload"));

        final VisualPlayerElement player = systemPlaylistScreen.clickHeaderPlay();

        assertTrue(player.isExpanded());

        mrLocalLocal.verify("specs/new_for_you_playback.spec");
    }

    // Note: I had to split this test into 2 to reduce flakiness caused by `player:max / player:min` click events.
    @Test
    public void testNewForYouEngagement() throws Exception {
        mrLocalLocal.startEventTracking();

        final SystemPlaylistScreen systemPlaylistScreen = new SystemPlaylistScreen(solo);

        assertThat(systemPlaylistScreen, is(visible()));
        assertThat("New for you screen title should be 'The Upload'",
                   systemPlaylistScreen.getActionBarTitle(),
                   equalTo("The Upload"));

        systemPlaylistScreen.toggleTrackLike(0);

        mrLocalLocal.verify("specs/new_for_you_engagement3.spec");
    }
}
