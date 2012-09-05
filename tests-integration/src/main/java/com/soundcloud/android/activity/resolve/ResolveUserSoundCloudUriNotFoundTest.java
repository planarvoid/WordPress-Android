package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.R;
import com.soundcloud.android.TestConsts;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.content.Intent;

public class ResolveUserSoundCloudUriNotFoundTest extends ActivityTestCase<Main> {

    public ResolveUserSoundCloudUriNotFoundTest() {
        super(Main.class);
    }

    @Override
    protected void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(TestConsts.UNRESOLVABLE_SC_USER_URI);
        setActivityIntent(intent);
        super.setUp();
    }

    public void testResolveUnknownUrlShouldShowErrorLoadingUrl() throws Exception {
        solo.assertText(R.string.error_loading_url);
    }
}