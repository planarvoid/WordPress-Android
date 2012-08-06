package com.soundcloud.android.activity.create;

import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_PLAYBACK;

import com.soundcloud.android.R;
import com.soundcloud.android.model.User;

import android.content.Intent;

public class PrivateRecordingTest extends AbstractRecordingTestCase {
    private User recipient;

    @Override
    public void setUp() throws Exception {
        recipient = new User();
        recipient.id       = 3090821; // jberkel_testing
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

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        solo.assertText(R.string.private_message_upload_title); // "Your sound message got saved."

        // Before we send it out privately to Super Hans, maybe you want to add some more info?
        solo.assertText(R.string.private_message_upload_message, recipient.username);

        solo.enterText(0, "Hallo Hans");

        solo.clickOnText(R.string.private_message_btn_send);
        assertSoundUploaded();
    }
}
