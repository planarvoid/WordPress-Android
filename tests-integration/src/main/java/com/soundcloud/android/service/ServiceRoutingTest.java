package com.soundcloud.android.service;

import com.soundcloud.android.service.playback.CloudPlaybackService;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;

public class ServiceRoutingTest extends InstrumentationTestCase {

    public void testServicePlayIntent() {
        ComponentName name =
                getInstrumentation().getTargetContext().startService(new Intent(CloudPlaybackService.PLAY_ACTION,
                        Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/me/tracks")));

        assertNotNull(name);
        assertEquals(CloudPlaybackService.class.getName(), name.getClassName());
    }

    public void testActionIntents() {
        for (String action : new String[]{
                CloudPlaybackService.TOGGLEPAUSE_ACTION,
                CloudPlaybackService.PAUSE_ACTION,
                CloudPlaybackService.NEXT_ACTION,
                CloudPlaybackService.PREVIOUS_ACTION,
                CloudPlaybackService.RESET_ALL,
                CloudPlaybackService.STOP_ACTION,
                CloudPlaybackService.ADD_LIKE_ACTION,
                CloudPlaybackService.REMOVE_LIKE_ACTION,
        }) {
            ComponentName name = getInstrumentation().getTargetContext().startService(new Intent(action));
            assertNotNull("action "+action+" not resolved", name);
            assertEquals(CloudPlaybackService.class.getName(), name.getClassName());
        }
    }
}
