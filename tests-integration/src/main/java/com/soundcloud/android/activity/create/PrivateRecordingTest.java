package com.soundcloud.android.activity.create;

import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_PLAYBACK;

import com.soundcloud.android.R;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.upload.UploadService;

import android.content.Intent;

public class PrivateRecordingTest extends RecordingTestCase {
    private User recipient;

    @Override
    public void setUp() throws Exception {
        recipient = new User();
        recipient.id       = 133201;
        recipient.username = "Super Hans";

        setActivityIntent(new Intent().putExtra(ScCreate.EXTRA_PRIVATE_MESSAGE_RECIPIENT, recipient));
        super.setUp();
    }

    public void testRecordAndPlayback() throws Exception {
        record(RECORDING_TIME, solo.getString(R.string.private_message_title, recipient.username)); // "Record a sound for Super Hans"
        playback();
        solo.sleep(RECORDING_TIME + 1000);
        assertState(IDLE_PLAYBACK);
    }

    public void testUploadPrivateMessage() {
        record(RECORDING_TIME, solo.getString(R.string.private_message_title, recipient.username));
        solo.clickOnButtonResId(R.string.btn_next);
        solo.assertActivity(ScUpload.class);

        solo.assertText(R.string.private_message_upload_title); // "Your sound message got saved."

        // Before we send it out privately to Super Hans, maybe you want to add some more info?
        solo.assertText(R.string.private_message_upload_message, recipient.username);

        solo.enterText(0, "Hallo Hans");

        solo.clickOnText(R.string.private_message_btn_send);
        assertTrue(waitForIntent(UploadService.UPLOAD_SUCCESS, 10000));
    }
}
