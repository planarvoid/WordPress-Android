package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;
import android.net.Uri;

public class ResolveTrackTest extends ActivityTest<ResolveActivity> {

    public ResolveTrackTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(getUri()));
        super.setUp();
    }

    public void testShouldLandOnLoginScreenForAnonymousUsers() {
        // We are not logged in
        assertThat(new HomeScreen(solo), is(visible()));
        assertTrue(waiter.expectToastWithText(toastObserver, "Please sign in to open this link"));
    }

    public void testShouldOpenPlayerFromDeeplinkAfterSignIn() {
        new HomeScreen(solo)
                .clickLogInButton()
                .loginAs(defaultUser.getEmail(), defaultUser.getPassword());

        VisualPlayerElement visualPlayer = new VisualPlayerElement(solo);
        assertThat(visualPlayer, is(expanded()));
        assertThat(visualPlayer.getTrackTitle(), is(equalToIgnoringCase("STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]")));
    }


    private Uri getUri() {
        return TestConsts.CHE_FLUTE_URI;
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
