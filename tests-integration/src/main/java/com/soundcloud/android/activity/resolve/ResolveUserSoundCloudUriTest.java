package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.content.Intent;

public class ResolveUserSoundCloudUriTest extends ActivityTestCase<Main> {

    public ResolveUserSoundCloudUriTest() {
        super(Main.class);
    }

    @Override
    protected void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(TestConsts.STEVE_ANGELLO_SC_URI);
        setActivityIntent(intent);
        super.setUp();
    }

    public void testResolveUrl() throws Exception {
        solo.assertActivity(UserBrowser.class);
        solo.assertText("steveangello");
    }
}