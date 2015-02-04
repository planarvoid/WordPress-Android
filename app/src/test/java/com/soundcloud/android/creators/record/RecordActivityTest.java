package com.soundcloud.android.creators.record;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.Actions;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class RecordActivityTest {

    private RecordActivity activity;

    @Before
    public void setUp() throws Exception {
        activity = new RecordActivity();
    }

    @Test
    public void shouldGoToStreamOnNavigationUp() throws Exception {
        activity.onSupportNavigateUp();
        Intent nextStartedActivity = shadowOf(activity).getNextStartedActivity();
        expect(nextStartedActivity.getAction()).toEqual(Actions.STREAM);
        expect(nextStartedActivity.getFlags()).toEqual(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Test
    public void shouldFinishOnNavigationUp() throws Exception {
        activity.onSupportNavigateUp();
        expect(activity.isFinishing()).toBeTrue();
    }
}
