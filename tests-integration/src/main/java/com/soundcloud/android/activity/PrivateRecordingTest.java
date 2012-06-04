package com.soundcloud.android.activity;

import static com.soundcloud.android.activity.ScCreate.CreateState.IDLE_PLAYBACK;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.model.User;
import com.soundcloud.android.service.upload.UploadService;
import junit.framework.Assert;

import android.content.Intent;
import android.test.suitebuilder.annotation.Suppress;

@Suppress
public class PrivateRecordingTest extends RecordingTestCase {
    @Override
    public void setUp() throws Exception {
        User recipient = new User();
        recipient.id       = 133201;
        recipient.username = "Super Hans";

        setActivityIntent(new Intent().putExtra(ScCreate.EXTRA_PRIVATE_MESSAGE_RECIPIENT, recipient));
        super.setUp();
    }

    public void testRecordAndPlayback() throws Exception {
        record(1000, "Record a sound for Super Hans");
        playback();
        solo.sleep(1500);
        assertState(IDLE_PLAYBACK);
    }

    public void testUploadPrivateMessage() {
        record(1000, "Record a sound for Super Hans");
        solo.clickOnButton("Next");
        solo.waitForActivity(ScUpload.class.getSimpleName());

        assertTrue(solo.waitForText("Your sound message got saved."));
        assertTrue(solo.waitForText("Before we send it out privately to Super Hans, maybe you want to add some more info?"));

        solo.enterText(0, "Hallo Hans");

        solo.clickOnText("Send");
        waitForIntent(UploadService.UPLOAD_SUCCESS, 10000);
    }
}
