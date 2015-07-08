package com.soundcloud.android.tests.service;

import com.soundcloud.android.playback.PlaybackService;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;

public class ServiceRoutingTest extends InstrumentationTestCase {

    public void ignore_testServicePlayIntent() {
        ComponentName name =
                getInstrumentation().getTargetContext().startService(new Intent(PlaybackService.Actions.PLAY_ACTION,
                        Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/me/tracks")));

        assertNotNull(name);
        assertEquals(PlaybackService.class.getName(), name.getClassName());
    }

    public void ignore_testActionIntents() {
        for (String action : new String[]{
                PlaybackService.Actions.TOGGLEPLAYBACK_ACTION,
                PlaybackService.Actions.PAUSE_ACTION,
                PlaybackService.Actions.RESET_ALL,
                PlaybackService.Actions.STOP_ACTION,
        }) {
            ComponentName name = getInstrumentation().getTargetContext().startService(new Intent(action));
            assertNotNull("action "+action+" not resolved", name);
            assertEquals(PlaybackService.class.getName(), name.getClassName());
        }
    }
}
