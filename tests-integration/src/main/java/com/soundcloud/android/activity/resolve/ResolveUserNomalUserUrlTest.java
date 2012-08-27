package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.activity.Main;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.content.Intent;
import android.net.Uri;

public class ResolveUserNomalUserUrlTest extends ActivityTestCase<Main> {

    public ResolveUserNomalUserUrlTest() {
        super(Main.class);
    }

    @Override
    protected void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());

        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("http://soundcloud.com/steveangello"));

        setActivityIntent(intent);
        super.setUp();
    }

    public void testResolveUrl() throws Exception {
        solo.assertActivity(UserBrowser.class);
        solo.assertText("steveangello");
    }
}
