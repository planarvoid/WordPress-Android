package com.soundcloud.android.activity.resolve;

import android.content.Intent;
import android.net.Uri;
import com.soundcloud.android.activity.ResolveActivity;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Waiter;

public class ResolveExplore extends ActivityTestCase<ResolveActivity> {
    protected static final int DEFAULT_WAIT = 30 * 1000;
    protected static PlayerScreen playerScreen;
    protected static ProfileScreen profileScreen;
    protected static Waiter waiter;

    public ResolveExplore() {
        super(ResolveActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/explore");
        IntegrationTestHelper.loginAsDefault(getInstrumentation());
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(uri));
        super.setUp();

        waiter = new Waiter(solo);
        profileScreen = new ProfileScreen(solo);
        playerScreen = new PlayerScreen(solo);
    }

    public void testResolveExploreUrl() throws Exception {
    }

    @Override
    public void tearDown() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.tearDown();
    }
}
