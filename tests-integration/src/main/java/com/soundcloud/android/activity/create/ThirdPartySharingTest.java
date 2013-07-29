package com.soundcloud.android.activity.create;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.test.ShareSound;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.AudioTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.ScAndroidTestCase;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ThirdPartySharingTest extends ActivityTestCase<ShareSound> {

    protected LocalBroadcastManager lbm;
    protected Map<String, Intent> intents;

    final private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intents.put(intent.getAction(), intent);
        }
    };

    public ThirdPartySharingTest() {
        super(ShareSound.class);
    }

    @Override public void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());
        intents = Collections.synchronizedMap(new LinkedHashMap<String, Intent>());
        lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(receiver, UploadService.getIntentFilter());
        super.setUp();
    }

    @Override public void tearDown() throws Exception {
        lbm.unregisterReceiver(receiver);
        super.tearDown();
    }

    /*
     * Create an empty temp file, send sharing intent, assert upload screen + header is shown, upload.
     */
    public void ignore_testShareWithExistingFile() throws IOException {
        File file = File.createTempFile("sharing", ".ogg", Environment.getExternalStorageDirectory());

        AssetManager am = ScAndroidTestCase.testAssets(getInstrumentation().getContext());
        IOUtils.copy(am.open(AudioTestCase.SHORT_TEST_OGG), file);

        Intent intent = getShareIntent(file, "Testing");
        intent.putExtra("com.soundcloud.android.extra.where",  "Somewhere");

        getActivity().setIntent(intent);
        solo.clickOnText(ShareSound.SHARE);
        solo.assertActivity(ScUpload.class);

        solo.assertText("Testing");
        solo.assertText("Somewhere");

//        solo.assertText(R.string.share_log_out);
        solo.assertNoText(R.string.record_another_sound);
        solo.clickOnText(R.string.post);

        solo.assertActivity(ShareSound.class);

        ShareSound.Result result = getActivity().getResult();
        assertNotNull(result);
        assertEquals(result.resultCode, Activity.RESULT_OK);
        assertNotNull(result.intent);
        assertNotNull(result.intent.getData());
        assertTrue(result.intent.hasExtra(Actions.UPLOAD_EXTRA_UPLOADING));

        assertMatches("content://com.soundcloud.android.provider.ScContentProvider/recordings/\\d+",
                result.intent.getData().toString());

        assertNotNull(waitForIntent(UploadService.TRANSCODING_SUCCESS, 15000));

        // make sure file didn't get deleted
        assertTrue("file should not get deleted", file.exists());
    }

    /*
     * Share a non existent file
     */
    public void ignore_testSharingWithMissingFile() {
        getActivity().setIntent(getShareIntent(new File("/hallo/no/file"), "Testing"));
        solo.clickOnText(ShareSound.SHARE);

        solo.sleep(3000);

        solo.assertActivity(ShareSound.class);
        ShareSound.Result result = getActivity().getResult();
        assertNotNull(result);
        assertEquals(result.resultCode, Activity.RESULT_OK);
        assertNull(result.intent);
    }

    public void ignore_testShareImplicit() {
        solo.clickOnText(ShareSound.SHARE_IMPLICIT);

        // make sure intent chooser pops up
        solo.assertText(ShareSound.SHARE_TO);

        solo.clickOnText(getInstrumentation().getTargetContext().getString(R.string.app_name));

        solo.assertActivity(ShareSound.class);
        ShareSound.Result result = getActivity().getResult();
        assertNotNull(result);
        assertEquals(result.resultCode, Activity.RESULT_OK);
        assertNull(result.intent);
    }

    private Intent getShareIntent(File file, String title) {
        return new Intent("com.soundcloud.android.SHARE")
                .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                .putExtra("com.soundcloud.android.extra.title", title);

    }

    protected @Nullable Intent waitForIntent(String action, long timeout) {
        final long startTime = SystemClock.uptimeMillis();
        final long endTime = startTime + timeout;
        while (SystemClock.uptimeMillis() < endTime) {
            solo.sleep(100);
            if (intents.containsKey(action)) {
                return intents.get(action);
            }
        }
        return null;
    }
}