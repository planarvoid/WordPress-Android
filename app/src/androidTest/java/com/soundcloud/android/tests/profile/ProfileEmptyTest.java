package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.playerUser;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;
import android.net.Uri;

public class ProfileEmptyTest extends ActivityTest<ResolveActivity> {

    private ProfileScreen screen;

    public ProfileEmptyTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return playerUser;
    }

    @Override
    protected void setUp() throws Exception {
        // cheap deeplink to the empty user
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/scEmpty")));
        super.setUp();
        screen = new ProfileScreen(solo);
    }

    public void testEmptyProfile() {
        assertTrue(screen.showsEmptySoundsMessage());
        assertTrue(screen.touchInfoTab().clickFollowingsLink().showsEmptyMessage());
    }

}
