package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.content.Intent;
import android.net.Uri;

public class ResolveTrackNormalUrlTest extends ActivityTestCase<Main> {

    public ResolveTrackNormalUrlTest() {
        super(Main.class);
    }

    @Override
    protected void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());

        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("http://soundcloud.com/steveangello/steve-angello-che-flute"));

        setActivityIntent(intent);
        super.setUp();
    }

    public void testResolveUrl() throws Exception {
        solo.assertActivity(ScPlayer.class);
        solo.assertText("CHE FLUTE");

        // make sure track doesn't keep playing in the background
        solo.clickOnView(R.id.pause);
    }
}
