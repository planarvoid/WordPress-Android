package com.soundcloud.android.activity.create;

import static com.soundcloud.android.activity.create.ScCreate.CreateState.EDIT;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.RECORD;

import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.R;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.view.create.TrimHandle;
import com.soundcloud.api.Env;

import android.content.Intent;
import android.net.Uri;
import android.test.suitebuilder.annotation.Suppress;


/**
 * Testing edit functions of the recording: trimming, appending, fading etc.
 */
public class RecordEditTest extends RecordingTestCase {


    /**
     * Record something, move trim handles, playback trimmed version.
     */
    public void testEditAndTrim() {
        record(RECORDING_TIME);
        gotoEditMode();

        trim(0.25, 0.25);

        solo.clickOnText(R.string.btn_apply);
        playback();
        waitForState(IDLE_PLAYBACK, RECORDING_TIME);

        solo.sleep(1000);
    }

    /**
     * Trim and append. Make sure the trimmed version of the uploaded file gets transcoded properly.
     */
    public void testEditAndTrimAndAppend() {

        record(RECORDING_TIME);
        gotoEditMode();
        trim(0.25, 0.25);
        solo.clickOnText(R.string.btn_apply);

        record(RECORDING_TIME);

        gotoEditMode();
        trim(0, 0.25);
        solo.clickOnText(R.string.btn_apply);

        record(RECORDING_TIME);

        solo.clickOnPublish();

        solo.assertActivity(ScUpload.class);

        solo.enterText(0, "An edit test upload");
        solo.clickOnButtonResId(R.string.sc_upload_private);
        solo.clickOnText(R.string.post);

        assertIntentAction(UploadService.UPLOAD_SUCCESS, 10000);
        if (env == Env.LIVE) {
            assertIntentAction(UploadService.TRANSCODING_SUCCESS, 30000);
        }
        solo.sleep(1000);
    }


    /*
     * Record something, enable fading and upload. Make sure transcoding works.
     */
    public void testFading() throws Exception {
        record(RECORDING_TIME);
        gotoEditMode();
        assertTrue(toggleFade());

        solo.clickOnText(R.string.btn_apply);

        solo.clickOnPublish();

        solo.assertActivity(ScUpload.class);

        solo.enterText(0, "A faded test upload");
        solo.clickOnButtonResId(R.string.sc_upload_private);
        solo.clickOnText(R.string.post);

        assertIntentAction(UploadService.UPLOAD_SUCCESS, 10000);
        if (env == Env.LIVE) {
            assertIntentAction(UploadService.TRANSCODING_SUCCESS, 30000);
        }
        solo.sleep(1000);
    }

    private void trim(double left, double right) {
        assertState(EDIT);
        TrimHandle leftTrim = (TrimHandle) solo.getView(TrimHandle.class, 0);
        TrimHandle rightTrim = (TrimHandle) solo.getView(TrimHandle.class, 1);
        int width = solo.getScreenWidth();
        if (left > 0)  solo.dragViewHorizontally(leftTrim ,  (int) (width * left), 5);
        if (right > 0) solo.dragViewHorizontally(rightTrim, -(int) (width * right), 5);
    }

    @Suppress
    public void testEditModesGetPersisted() {
        record(RECORDING_TIME);
        gotoEditMode();

        playbackEdit();

        solo.clickOnText("No Fading");
        solo.clickOnText("Not Optimized");
        solo.clickOnText("Save");

        assertState(ScCreate.CreateState.IDLE_PLAYBACK);

        assertTrue(solo.isToggleButtonChecked("Fades on"));
        assertTrue(solo.isToggleButtonChecked("Optimized"));

        Uri recUri = getActivity().getRecorder().getRecording().toUri();
        assertNotNull(recUri);

        // start new activity just with uri
        ScCreate create = launchActivityWithIntent("com.soundcloud.android", ScCreate.class, new Intent().setData(recUri));
        assertSame(ScCreate.CreateState.IDLE_PLAYBACK, create.getState());
        Solo solo2 = new Solo(getInstrumentation(), create);
        solo2.clickOnView(create.findViewById(R.id.btn_edit));
        solo2.sleep(100);
        assertSame(ScCreate.CreateState.EDIT, create.getState());

        assertTrue(solo2.isToggleButtonChecked("Fades on"));
        assertTrue(solo2.isToggleButtonChecked("Optimized"));
    }
}
