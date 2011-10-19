package com.soundcloud.android.activity;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import com.soundcloud.android.Consts;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class ScPlayerTest {
    @Test
    public void onResumeShouldTrackPlayer() throws Exception {
        assertThat(DefaultTestRunner.application.trackedPages, not(hasItem(Consts.Tracking.PLAYER)));
        new ScPlayer() { { onResume(); } };
        assertThat(DefaultTestRunner.application.trackedPages, hasItem(Consts.Tracking.PLAYER));
    }
}
