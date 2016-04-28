package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.playerUser;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;
import android.net.Uri;

@Ignore
public class OtherProfileEmptyTest extends ActivityTest<ResolveActivity> {

    private ProfileScreen screen;

    public OtherProfileEmptyTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() {
        playerUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        // cheap deeplink to the empty user
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/scEmpty")));
        super.setUp();
        screen = new ProfileScreen(solo);
    }

    public void testShowsEmptyInfoView() {
        screen.touchInfoTab();
        assertTrue(screen.showsEmptyInfoMessage());
    }

    public void testShowsEmptyFollowingView() {
        screen.touchFollowingsTab();
        assertTrue(screen.showsEmptyFollowingsMessage());
    }

    public void testShowsEmptySoundsMessage() {
        assertTrue(screen.showsEmptySoundsMessage());
    }

}
