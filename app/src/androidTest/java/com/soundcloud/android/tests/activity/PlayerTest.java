package com.soundcloud.android.tests.activity;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class PlayerTest extends ActivityTest<MainActivity> {
    private StreamScreen streamScreen;

    public PlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return streamUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        streamScreen = new StreamScreen(solo);
    }

    @Test
    public void testVisualPlayerIsAccessible() throws Exception {
        final VisualPlayerElement player = streamScreen.clickFirstTrackCard();
        assertThat(player, is(expanded()));

        player.pressBackToCollapse();

        mainNavHelper.goToActivities();
        assertThat(player, is(visible()));
        assertThat(player, is(collapsed()));
    }

    @Test
    public void testPlayerIsNotVisibleIfNothingIsPlaying() throws Exception {
        mainNavHelper.goToActivities();

        assertThat(new VisualPlayerElement(solo), is(not(visible())));
    }
}
