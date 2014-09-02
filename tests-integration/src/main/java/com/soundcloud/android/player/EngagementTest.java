package com.soundcloud.android.player;

import static com.soundcloud.android.tests.matcher.view.IsEnabled.Enabled;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.PlayerMenuElement;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class EngagementTest extends ActivityTestCase<MainActivity> {

    public EngagementTest() {
        super(MainActivity.class);
    }

    public void setUp() throws Exception {
        TestUser.privateUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testPrivateTrackHasDisabledShareAndRepost() {
        PlayerMenuElement menu = menuScreen.open()
                .clickUserProfile()
                .playTrack(0)
                .clickMenu();

        assertThat(menu.repostItem(), is(not(Enabled())));
        assertThat(menu.shareItem(), is(not(Enabled())));
    }

    public void testPublicTrackHasEnabledShareAndRepost() {
        PlayerMenuElement menu = menuScreen.open()
                .clickLikes()
                .clickItem(1)
                .clickMenu();

        assertThat(menu.repostItem(), is(Enabled()));
        assertThat(menu.shareItem(), is(Enabled()));
    }
}
