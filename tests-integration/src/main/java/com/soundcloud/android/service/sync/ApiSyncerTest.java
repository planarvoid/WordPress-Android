package com.soundcloud.android.service.sync;

import com.soundcloud.android.provider.Content;

import android.content.Intent;
import android.test.InstrumentationTestCase;

public class ApiSyncerTest extends InstrumentationTestCase {

    public void testUserCleanup() throws Exception {
        ApiSyncer syncer = new ApiSyncer(getInstrumentation().getContext());
        long start = System.currentTimeMillis();
        syncer.syncContent(Content.USERS_CLEANUP.uri, Intent.ACTION_SYNC);
        long duration = System.currentTimeMillis() - start;
        assertTrue("query ran too long ("+duration+")", duration < 5000);
    }


    public void testTrackCleanup() throws Exception {
        ApiSyncer syncer = new ApiSyncer(getInstrumentation().getContext());
        long start = System.currentTimeMillis();
        syncer.syncContent(Content.TRACK_CLEANUP.uri, Intent.ACTION_SYNC);
        long duration = System.currentTimeMillis() - start;
        assertTrue("query ran too long ("+duration+")", duration < 5000);
    }
}
