package com.soundcloud.android.activity;


import static com.soundcloud.android.activity.ScCreate.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.activity.ScCreate.CreateState.IDLE_RECORD;

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

        solo.clickOnText("Revert to original");
        solo.waitForText("You will lose all of your edits.");
        solo.clickOnText("OK");

        assertState(IDLE_PLAYBACK);
    }

    public void testRecordAndEditSaveAndDelete() throws Exception {
        record(RECORDING_TIME);
        gotoEditMode();

        solo.clickOnText("Save");
        assertState(IDLE_PLAYBACK);

        solo.clickOnText("Delete");
        assertTrue(solo.waitForText("Are you sure you want to delete this recording?"));
        solo.clickOnText("OK");
        solo.sleep(500);
        assertTrue(getActivity().isFinishing());
    }

    public void testRecordAndDiscard() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnText("Discard");
        assertTrue(solo.waitForText("Reset?"));
        solo.clickOnText("OK");
        assertState(IDLE_RECORD);
    }

    public void testRecordAndUpload() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnButton("Next");
        assertTrue(solo.waitForActivity(ScUpload.class.getSimpleName()));

        solo.enterText(0, "A test upload");
        solo.clickOnRadioButton(1);  // make it private
        solo.clickOnText("Upload & Share");

        assertTrue("did not get upload notification", waitForIntent(UploadService.UPLOAD_SUCCESS, 10000));
        assertTrue(getActivity().isFinishing());
    }


    public void testRecordAndUploadThenRecordAnotherSound() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnButton("Next");
        assertTrue(solo.waitForActivity(ScUpload.class.getSimpleName()));

        solo.clickOnText("Record another sound");

        assertTrue(solo.waitForActivity(ScCreate.class.getSimpleName()));
        assertState(IDLE_RECORD); // should be read to record a new track
    }

    public void testRecordAndUploadThenGoBack() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnButton("Next");
        assertTrue(solo.waitForActivity(ScUpload.class.getSimpleName()));

        solo.goBack();

        assertTrue(solo.waitForActivity(ScCreate.class.getSimpleName()));
        assertState(IDLE_PLAYBACK); // should be old recording
    }
}
