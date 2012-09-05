package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.R;
import com.soundcloud.android.TestConsts;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.content.Intent;
import android.net.Uri;

public class ResolveTrackSoundCloudUriNotFoundTest extends ActivityTestCase<Main> {

    public ResolveTrackSoundCloudUriNotFoundTest() {
        super(Main.class);
    }

    @Override
    protected void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(TestConsts.UNRESOLVABLE_SC_TRACK_URI);
        setActivityIntent(intent);
        super.setUp();
    }

    public void testResolveUnknownUrlShouldShowErrorLoadingUrl() throws Exception {
        solo.assertText(R.string.error_loading_url);
    }
}