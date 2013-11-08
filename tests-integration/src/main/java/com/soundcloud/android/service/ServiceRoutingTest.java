package com.soundcloud.android.service;

import com.soundcloud.android.playback.service.CloudPlaybackService;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;

public class ServiceRoutingTest extends InstrumentationTestCase {

    public void ignore_testServicePlayIntent() {
        ComponentName name =
                getInstrumentation().getTargetContext().startService(new Intent(CloudPlaybackService.Actions.PLAY_ACTION,
                        Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/me/tracks")));

        assertNotNull(name);
        assertEquals(CloudPlaybackService.class.getName(), name.getClassName());
    }

    public void ignore_testActionIntents() {
        for (String action : new String[]{
                CloudPlaybackService.Actions.TOGGLEPLAYBACK_ACTION,
                CloudPlaybackService.Actions.PAUSE_ACTION,
                CloudPlaybackService.Actions.NEXT_ACTION,
                CloudPlaybackService.Actions.PREVIOUS_ACTION,
                CloudPlaybackService.Actions.RESET_ALL,
                CloudPlaybackService.Actions.STOP_ACTION,
                CloudPlaybackService.Actions.ADD_LIKE_ACTION,
                CloudPlaybackService.Actions.REMOVE_LIKE_ACTION,
        }) {
            ComponentName name = getInstrumentation().getTargetContext().startService(new Intent(action));
            assertNotNull("action "+action+" not resolved", name);
            assertEquals(CloudPlaybackService.class.getName(), name.getClassName());
        }
    }
}
