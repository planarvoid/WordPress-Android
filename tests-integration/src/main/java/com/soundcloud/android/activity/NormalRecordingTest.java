package com.soundcloud.android.activity;


import static com.soundcloud.android.activity.ScCreate.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.activity.ScCreate.CreateState.IDLE_RECORD;
import static com.soundcloud.android.activity.ScCreate.CreateState.PLAYBACK;

import com.soundcloud.android.service.upload.UploadService;

import android.test.suitebuilder.annotation.Suppress;

//@Suppress
public class NormalRecordingTest extends RecordingTestCase {

    public void testRecordAndPlayback() throws Exception {
        record(1000);
        playback();
        solo.sleep(100);
        assertState(PLAYBACK);
        solo.sleep(1500);
        assertState(IDLE_PLAYBACK);
    }

    public void testRecordAndEditRevert() throws Exception {
        record(1000);
        gotoEditMode();

        solo.clickOnText("Revert to original");
        solo.waitForText("You will lose all of your edits.");
        solo.clickOnText("OK");

        assertState(IDLE_PLAYBACK);
    }

    public void testRecordAndEditSaveAndDelete() throws Exception {
        record(1000);
        gotoEditMode();

        solo.clickOnText("Save");
        assertState(IDLE_PLAYBACK);

        solo.clickOnText("Delete");
        solo.waitForText("Are you sure you want to delete this recording?");
        solo.clickOnText("OK");
        solo.sleep(500);
        assertTrue(getActivity().isFinishing());
    }

    public void testRecordAndDiscard() throws Exception {
        record(1000);

        solo.clickOnText("Discard");
        solo.waitForText("Reset? Recording will be deleted.");
        solo.clickOnText("OK");
        solo.sleep(100);
        assertState(IDLE_RECORD);
    }

    public void testRecordAndUpload() throws Exception {
        record(1000);

        solo.clickOnButton("Next");
        solo.waitForActivity(ScUpload.class.getSimpleName());

        solo.enterText(0, "A test upload");
        solo.clickOnRadioButton(1);  // make it private
        solo.clickOnText("Upload & Share");

        waitForIntent(UploadService.UPLOAD_SUCCESS, 10000);
        assertTrue(getActivity().isFinishing());
    }


    public void testRecordAndUploadThenRecordAnotherSound() throws Exception {
        record(1000);

        solo.clickOnButton("Next");
        solo.waitForActivity(ScUpload.class.getSimpleName());

        solo.clickOnText("Record another sound");

        solo.waitForActivity(ScCreate.class.getSimpleName());

        solo.sleep(400);
        assertState(IDLE_RECORD); // should be read to record a new track
    }

    public void testRecordAndUploadThenGoBack() throws Exception {
        record(1000);

        solo.clickOnButton("Next");
        solo.waitForActivity(ScUpload.class.getSimpleName());

        solo.goBack();

        solo.waitForActivity(ScCreate.class.getSimpleName());

        solo.sleep(400);
        assertState(IDLE_PLAYBACK); // should be old recording
    }
}
