package com.soundcloud.android.activity.create;

import com.soundcloud.android.R;

import android.widget.EditText;

public class ShareRecordingTest extends AbstractRecordingTestCase {

    public void testRecordAndSharePrivatelyToEmailAddress() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        solo.enterText(0, "A test upload");
        solo.clickOnButtonResId(R.string.sc_upload_private);

        solo.clickOnText(R.string.sc_upload_only_you);

        solo.assertActivity(EmailPicker.class);

        solo.enterText(0, "recipient@example.com");
        solo.clickOnOK();

        solo.assertActivity(ScUpload.class);

        solo.assertNoText(R.string.sc_upload_only_you);
        solo.assertText("recipient@example.com");
        solo.clickOnText("recipient@example.com");

        solo.assertActivity(EmailPicker.class);
        solo.assertText("recipient@example.com");

        solo.clickOnButtonResId(R.string.email_picker_clear);
        solo.clickOnOK();

        solo.assertActivity(ScUpload.class);
        solo.assertText(R.string.sc_upload_only_you);
    }

    public void testRecordAndSharePrivatelyToMultipleEmailAddresses() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        solo.enterText(0, "A test upload");
        solo.clickOnButtonResId(R.string.sc_upload_private);

        solo.clickOnText(R.string.sc_upload_only_you);

        solo.assertActivity(EmailPicker.class);

        solo.enterText(0, "recipient@example.com, another@example.com, foo@example.com");
        solo.clickOnOK();

        solo.assertActivity(ScUpload.class);

        solo.assertNoText(R.string.sc_upload_only_you);
        solo.assertText("recipient@example.com");
        solo.assertText("another@example.com");
        solo.assertText("foo@example.com");

        solo.clickOnText("another@example.com");

        solo.assertActivity(EmailPicker.class);
        solo.assertText("recipient@example.com");
        solo.assertText("another@example.com");
        solo.assertText("foo@example.com");

        EditText email = (EditText) solo.getView(R.id.email);
        assertEquals("cursor is not positioned correctly",
                "recipient@example.com".length() +
                        "another@example.com".length() + 2,

                email.getSelectionStart() );
        solo.clickOnOK();
        solo.assertActivity(ScUpload.class);
    }
}
