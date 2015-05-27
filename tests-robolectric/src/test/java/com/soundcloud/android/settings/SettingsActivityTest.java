package com.soundcloud.android.settings;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.Actions;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class SettingsActivityTest {

    private SettingsActivity activity;

    @Before
    public void setUp() throws Exception {
        activity = new SettingsActivity();
    }

    @Test
    public void goesToStreamOnNavigationUp() {
        activity.onSupportNavigateUp();
        Intent nextStartedActivity = shadowOf(activity).getNextStartedActivity();
        expect(nextStartedActivity.getAction()).toEqual(Actions.STREAM);
        expect(nextStartedActivity.getFlags()).toEqual(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Test
    public void finishesOnNavigationUp() {
        activity.onSupportNavigateUp();
        expect(activity.isFinishing()).toBeTrue();
    }

}
