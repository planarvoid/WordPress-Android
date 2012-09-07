package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.R;
import com.soundcloud.android.TestConsts;
import com.soundcloud.android.activity.ScPlayer;

import android.app.Instrumentation;
import android.content.IntentFilter;
import android.net.Uri;

public class ResolveTrackSoundCloudUriTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_SC_URI;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        getInstrumentation().addMonitor(new Instrumentation.ActivityMonitor((IntentFilter) null, null, false) {



        });
    }

    public void testResolveUrl() throws Exception {

        boolean found = solo.waitForActivity(ScPlayer.class.getSimpleName());

        if (!found) {
            System.err.println("not found");
        }

        //solo.assertActivity(ScPlayer.class, DEFAULT_WAIT);

        solo.assertText("CHE FLUTE");

        // make sure track doesn't keep playing in the background
        solo.clickOnView(R.id.pause);
    }
}
