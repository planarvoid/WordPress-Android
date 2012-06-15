package com.soundcloud.android.activity.create;


import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_RECORD;

import com.soundcloud.android.R;
import com.soundcloud.android.service.upload.UploadService;

public class NormalRecordingTest extends RecordingTestCase {

    public void testRecordAndPlayback() throws Exception {
        record(RECORDING_TIME);
        playback();
        solo.sleep(RECORDING_TIME + 500);
        assertState(IDLE_PLAYBACK);
    }

    public void testRecordAndEditRevert() throws Exception {
        record(RECORDING_TIME);
        gotoEditMode();

        solo.clickOnText(R.string.btn_revert_to_original);
        solo.assertText(R.string.dialog_revert_recording_message);
        solo.clickOnOK();

        assertState(IDLE_PLAYBACK);
    }

    public void testRecordAndEditSaveAndDelete() throws Exception {
        record(RECORDING_TIME);
        gotoEditMode();

        solo.clickOnText(R.string.btn_save);
        assertState(IDLE_PLAYBACK);

        solo.clickOnText(R.string.delete);
        solo.assertText(R.string.dialog_confirm_delete_recording_message);
        solo.clickOnOK();
        solo.sleep(500);
        solo.assertActivityFinished();
    }

    public void testRecordAndDiscard() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnText(R.string.reset); // "Discard"
        solo.assertText(R.string.dialog_reset_recording_message); // "Reset? Recording will be deleted."
        solo.clickOnOK();
        assertState(IDLE_RECORD);
    }

    public void testRecordAndUpload() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnNext();
        solo.assertActivity(ScUpload.class);

        solo.enterText(0, "A test upload");
        solo.clickOnRadioButton(1);  // make it private
        solo.clickOnText(R.string.upload_and_share);

        assertTrue("did not get upload notification", waitForIntent(UploadService.UPLOAD_SUCCESS, 10000));
        solo.assertActivityFinished();
    }


    public void testRecordAndUploadThenRecordAnotherSound() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnNext();
        solo.assertActivity(ScUpload.class);

        solo.clickOnText(R.string.record_another_sound);

        solo.assertActivity(ScCreate.class);
        assertState(IDLE_RECORD); // should be read to record a new track
    }

    public void testRecordAndUploadThenGoBack() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnNext();
        solo.assertActivity(ScUpload.class);

        solo.goBack();

        solo.assertActivity(ScCreate.class);
        assertState(IDLE_PLAYBACK); // should be old recording
    }
}
