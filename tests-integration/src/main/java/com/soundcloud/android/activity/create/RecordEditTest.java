package com.soundcloud.android.activity.create;

import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_PLAYBACK;

import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.tests.SlowTest;

import android.content.Intent;
import android.net.Uri;
import android.test.FlakyTest;
import android.test.suitebuilder.annotation.Suppress;


/**
 * Testing edit functions of the recording: trimming, appending, fading etc.
 */
@SlowTest
public class RecordEditTest extends AbstractRecordingTestCase {


    /**
     * Record something, move trim handles, playback trimmed version.
     */
    @FlakyTest
    public void testEditAndTrim() {
        record(RECORDING_TIME);
        gotoEditMode();

        trim(0.25, 0.25);

        applyEdits();

        solo.sleep(2000);

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

        assertSoundEncoded(RECORDING_TIME * 4);
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
        assertTrackDuration(track, 5000 + ROBO_SLEEP);
    }

    public void testTrimAndFadeAndUpload() throws Exception {
        record(10000);

        gotoEditMode();
        trim(0.25, 0.25);
        assertTrue(toggleFade());

        applyEdits();

        uploadSound("A faded + trimmed test upload", null, true);

        assertSoundEncoded(RECORDING_TIME * 4);
        assertSoundUploaded();
        Track track = assertSoundTranscoded();
        assertTrackDuration(track, 5000 + ROBO_SLEEP);
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
