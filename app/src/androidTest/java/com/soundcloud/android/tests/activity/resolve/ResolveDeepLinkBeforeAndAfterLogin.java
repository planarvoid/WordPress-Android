package com.soundcloud.android.tests.activity.resolve;

import static android.content.Intent.ACTION_VIEW;
import static com.soundcloud.android.R.string.error_toast_user_not_logged_in;
import static com.soundcloud.android.framework.TestUser.playerUser;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.CHE_FLUTE_TRACK_PERMALINK;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import android.content.Intent;

public class ResolveDeepLinkBeforeAndAfterLogin extends ActivityTest<ResolveActivity> {

    public ResolveDeepLinkBeforeAndAfterLogin() {
        super(ResolveActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setActivityIntent(new Intent(ACTION_VIEW).setData(CHE_FLUTE_TRACK_PERMALINK));
        super.setUp();
    }

    @Test
    public void testShouldLandOnLoginScreenForAnonymousUsers() throws Exception {
        // We are not logged in
        assertThat(new HomeScreen(solo), is(visible()));
        assertTrue(waiter.expectToastWithText(toastObserver, resourceString(error_toast_user_not_logged_in)));
    }

    @Test
    public void testShouldOpenPlayerFromDeeplinkAfterSignIn() throws Exception {
        assertThat(new HomeScreen(solo), is(visible()));
        new HomeScreen(solo)
                .clickLogInButton()
                .loginDefault(playerUser.getEmail(), playerUser.getPassword());

        VisualPlayerElement visualPlayer = new VisualPlayerElement(solo);
        visualPlayer.waitForExpandedPlayer();
        assertThat(visualPlayer, is(expanded()));
        assertThat(visualPlayer.getTrackTitle(), is(equalTo("STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]")));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
