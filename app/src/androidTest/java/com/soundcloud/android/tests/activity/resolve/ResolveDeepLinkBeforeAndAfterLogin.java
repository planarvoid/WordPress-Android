package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.TestUser.playerUser;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

public class ResolveDeepLinkBeforeAndAfterLogin extends ActivityTest<ResolveActivity> {

    public ResolveDeepLinkBeforeAndAfterLogin() {
        super(ResolveActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(TestConsts.CHE_FLUTE_TRACK_PERMALINK));
        super.setUp();
    }

    public void testShouldLandOnLoginScreenForAnonymousUsers() {
        // We are not logged in
        assertThat(new HomeScreen(solo), is(visible()));
        assertTrue(waiter.expectToastWithText(toastObserver, resourceString(R.string.error_toast_user_not_logged_in)));
    }

    public void testShouldOpenPlayerFromDeeplinkAfterSignIn() {
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
