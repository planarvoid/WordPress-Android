package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.matcher.view.IsEnabled.Enabled;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.PlayerHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.PlayerMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class EngagementTest extends ActivityTest<MainActivity> {

    public EngagementTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.privateUser;
    }

    public void testPrivateTrackHasDisabledShareAndRepost() {
        PlayerMenuElement menu = mainNavHelper.goToMyProfile()
                                              .playTrack(0)
                                              .clickMenu();

        assertThat(menu.repostItem(), is(not(Enabled())));
        assertThat(menu.shareItem(), is(not(Enabled())));
    }

    public void testLikeTrackAlwaysShowsTheShareButton() {
        VisualPlayerElement player = PlayerHelper.playPublicTrack(this, mainNavHelper);

        player.likeButton().click();
        assertThat(player.shareButton(), is(visible()));
    }

}
