package com.soundcloud.android.service.sync;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.tests.NonUiTest;

import android.content.Context;
import android.content.Intent;
import android.test.InstrumentationTestCase;

@NonUiTest
public class ApiSyncerTest extends InstrumentationTestCase {

    public void testUserCleanup() throws Exception {
        Context context = getInstrumentation().getTargetContext();
        ApiSyncer syncer = new ApiSyncer(context, context.getContentResolver());
        long start = System.currentTimeMillis();
        syncer.syncContent(Content.USERS_CLEANUP.uri, Intent.ACTION_SYNC);
        long duration = System.currentTimeMillis() - start;
        assertTrue("query ran too long ("+duration+")", duration < 5000);
    }


    public void testTrackCleanup() throws Exception {
        Context context = getInstrumentation().getTargetContext();
        ApiSyncer syncer = new ApiSyncer(context, context.getContentResolver());
        long start = System.currentTimeMillis();
        syncer.syncContent(Content.PLAYABLE_CLEANUP.uri, Intent.ACTION_SYNC);
        long duration = System.currentTimeMillis() - start;
        assertTrue("query ran too long ("+duration+")", duration < 5000);
    }
}
