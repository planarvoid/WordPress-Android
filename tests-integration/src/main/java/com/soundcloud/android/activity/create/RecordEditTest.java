package com.soundcloud.android.activity.create;

import static com.soundcloud.android.activity.create.ScCreate.CreateState.EDIT;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_PLAYBACK;

import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.create.TrimHandle;

import android.content.Intent;
import android.net.Uri;
import android.test.suitebuilder.annotation.Suppress;


/**
 * Testing edit functions of the recording: trimming, appending, fading etc.
 */
public class RecordEditTest extends AbstractRecordingTestCase {


    /**
     * Record something, move trim handles, playback trimmed version.
     */
    public void testEditAndTrim() {
        record(RECORDING_TIME);
        gotoEditMode();

        trim(0.25, 0.25);

        applyEdits();
        playback();
        waitForState(IDLE_PLAYBACK, RECORDING_TIME);

    }

    /**
     * Trim and append. Make sure the trimmed version of the uploaded file gets transcoded properly.
     */
    public void testEditAndTrimAndAppendAndUpload() {

        record(RECORDING_TIME);
        gotoEditMode();
        trim(0.25, 0.25);
        applyEdits();

        record(RECORDING_TIME);

        gotoEditMode();
        trim(0, 0.25);
        applyEdits();

        record(RECORDING_TIME);

        uploadSound("An edit test upload", null, true);

        assertSoundUploaded();
        assertSoundTranscoded();
    }


    /*
     * Record something, enable fading and upload. Make sure transcoding works.
     */
    public void testFadingAndUpload() throws Exception {
        record(RECORDING_TIME);
        gotoEditMode();
        assertTrue(toggleFade());

        applyEdits();

        uploadSound("A faded test upload", null, true);

        assertSoundUploaded();
        assertSoundTranscoded();
    }


    public void testTrimAndUpload() throws Exception {
        record(10000);

        gotoEditMode();
        trim(0.25, 0.25);

        applyEdits();

        uploadSound("A trimmed test upload", null, true);

        assertSoundUploaded();
        Track track = assertSoundTranscoded();
        if (track != null) {
            assertTrue("track duration is 0", track.duration > 0);
            if (!EMULATOR) {
                assertEquals("track duration is off", 5000, track.duration, 2000);
            }
        }
    }

    public void testTrimAndFadeAndUpload() throws Exception {
        record(10000);

        gotoEditMode();
        trim(0.25, 0.25);
        assertTrue(toggleFade());

        applyEdits();

        uploadSound("A faded + trimmed test upload", null, true);

        assertSoundUploaded();
        Track track = assertSoundTranscoded();
        if (track != null) {
            assertTrue("track duration is 0", track.duration > 0);
            if (!EMULATOR) {
                assertEquals("track duration is off", 5000, track.duration, 2000);
            }
        }
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
