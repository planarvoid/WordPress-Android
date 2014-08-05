package com.soundcloud.android.whoToFollow;

import static com.soundcloud.android.tests.helpers.NavigationHelper.openWhoToFollow;
import static com.soundcloud.android.tests.matcher.player.IsCollapsed.Collapsed;
import static com.soundcloud.android.tests.matcher.player.IsExpanded.Expanded;
import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class Player extends ActivityTestCase<MainActivity> {
    private VisualPlayerElement player;
    private StreamScreen streamScreen;

    public Player() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    public void testVisualPlayerIsAccessible() throws Exception {
        player = streamScreen.clickFirstTrack();
        assertThat(player, is(Expanded()));
        player.pressBackToCollapse();

        openWhoToFollow(streamScreen);
        assertThat(player, is(Visible()));
        assertThat(player, is(Collapsed()));
    }

    public void testPlayerIsNotVisibleIfNothingIsPlaying() throws Exception {
        openWhoToFollow(streamScreen);

        assertThat(new VisualPlayerElement(solo), is(not(Visible())));
    }
}
