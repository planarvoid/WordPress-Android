package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;

public abstract class ResolveTrackTest extends ResolveBaseTest {

    public void ignore_testResolveUrl() throws Exception {
        solo.assertActivity(ScPlayer.class, DEFAULT_WAIT);

        solo.assertText("CHE FLUTE");

        // make sure track doesn't keep playing in the background
        solo.clickOnView(R.id.pause);
    }
}