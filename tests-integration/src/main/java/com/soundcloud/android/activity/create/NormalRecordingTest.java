package com.soundcloud.android.activity.create;


import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_RECORD;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.service.upload.UploadService;

import android.content.Intent;
import android.test.suitebuilder.annotation.Suppress;
import android.widget.EditText;

import java.io.File;

public class NormalRecordingTest extends RecordingTestCase {

    public void testRecordAndPlayback() throws Exception {
        record(RECORDING_TIME);
        playback();
        solo.sleep(RECORDING_TIME + 500);
        assertState(IDLE_PLAYBACK);
    }

    public void testRecordMakeSureFilesGetWritten() throws Exception {
        record(RECORDING_TIME);
        Recording r = getActivity().getRecorder().getRecording();

        File raw = r.getFile();
        File encoded = r.getEncodedFile();

        assertTrue(raw.exists());
        assertTrue(encoded.exists());

        assertTrue(raw.length() > 100000);
        assertTrue(encoded.length() > 20000);
    }

    public void testRecordAndEditRevert() throws Exception {
        record(RECORDING_TIME);
        gotoEditMode();

        solo.clickOnText(R.string.btn_revert_to_original);
        solo.assertText(R.string.dialog_revert_recording_message);
        solo.clickOnOK();

        assertState(IDLE_PLAYBACK);
    }

    public void testRecordAndEditApplyAndDelete() throws Exception {
        record(RECORDING_TIME);
        gotoEditMode();

        solo.clickOnText(R.string.btn_apply);
        assertState(IDLE_PLAYBACK);

        solo.clickOnText(R.string.delete);
        solo.assertText(R.string.dialog_confirm_delete_recording_message);
        solo.clickOnOK();
        solo.sleep(500);
        solo.assertActivityFinished();
    }

    @Suppress // autosave is now in place
    public void testRecordAndDiscard() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnText(R.string.reset); // "Discard"
        solo.assertText(R.string.dialog_reset_recording_message); // "Reset? Recording will be deleted."
        solo.clickOnOK();
        assertState(IDLE_RECORD);
    }

    public void testRecordAndDelete() throws Exception {
        record(RECORDING_TIME);
        solo.clickOnText(R.string.delete); // "Discard"
        solo.assertText(R.string.dialog_confirm_delete_recording_message); // "Are you sure you want to delete this recording?"
        solo.clickOnOK();
        solo.assertActivityFinished();
    }

    public void testRecordAndUpload() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        solo.enterText(0, "A test upload");
        solo.clickOnButtonResId(R.string.sc_upload_private);
        solo.clickOnText(R.string.upload_and_share);

        assertTrue("did not get upload notification", waitForIntent(UploadService.UPLOAD_SUCCESS, 10000));
        solo.assertActivityFinished();
    }

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

    public void testRecordAndUploadThenRecordAnotherSound() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        solo.clickOnText(R.string.record_another_sound);

        solo.assertActivity(ScCreate.class);
        assertState(IDLE_RECORD); // should be read to record a new track
    }

    public void testRecordAndUploadThenGoBack() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        solo.goBack();

        solo.assertActivity(ScCreate.class);
        assertState(IDLE_PLAYBACK); // should be old recording
    }

    @Suppress
    public void testRecordAndLoadAndAppend() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnPublish();

        long id = System.currentTimeMillis();
        final String name = "A test upload " + id;
        solo.enterText(0, name);

        solo.assertActivity(ScUpload.class);

        solo.finishOpenedActivities();

        Main main = launchActivityWithIntent("com.soundcloud.android",
            Main.class, new Intent().putExtra(Main.TAB_TAG, Main.Tab.PROFILE.tag));


        solo.clickOnText(name);

        solo.sleep(500);

//        solo.assertActivity(ScCreate.class);

        record(RECORDING_TIME);

        solo.sleep(5000);

    }
}
