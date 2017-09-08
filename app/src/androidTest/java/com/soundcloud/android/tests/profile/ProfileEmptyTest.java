package com.soundcloud.android.tests.profile;

import static android.content.Intent.ACTION_VIEW;
import static android.net.Uri.parse;
import static com.soundcloud.android.framework.TestUser.playerUser;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

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
    public void setUp() throws Exception {
        // cheap deeplink to the empty user
        setActivityIntent(new Intent(ACTION_VIEW).setData(parse("http://soundcloud.com/scEmpty")));
        super.setUp();
        screen = new ProfileScreen(solo);
    }

    @Test
    public void testEmptyProfile() throws Exception {
        assertTrue(screen.showsEmptySoundsMessage());
        assertTrue(screen.touchInfoTab().clickFollowingsLink().showsEmptyMessage());
    }

}
