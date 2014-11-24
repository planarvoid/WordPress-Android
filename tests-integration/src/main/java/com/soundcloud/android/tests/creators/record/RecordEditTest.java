package com.soundcloud.android.tests.creators.record;

import static com.soundcloud.android.creators.record.RecordActivity.CreateState.IDLE_PLAYBACK;

import com.robotium.solo.Solo;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.tests.SlowTest;
import com.soundcloud.android.framework.with.With;

import android.content.Intent;
import android.net.Uri;
import android.test.FlakyTest;
import android.test.suitebuilder.annotation.Suppress;


/**
 * Testing edit functions of the recording: trimming, appending, fading etc.
 */
@SlowTest
public class RecordEditTest extends AbstractRecordingTest {


    /**
     * Record something, move trim handles, playback trimmed version.
     */
    @FlakyTest
    public void ignore_testEditAndTrim() {
        record(recordingTime);
        gotoEditMode();

        trim(0.25, 0.25);

        applyEdits();

        solo.sleep(2000);

        playback();
        waitForState(IDLE_PLAYBACK, recordingTime);

    }

    /**
     * Trim and append. Make sure the trimmed version of the uploaded file gets transcoded properly.
     */
    public void ignore_testEditAndTrimAndAppendAndUpload() {

        record(recordingTime);
        gotoEditMode();
        trim(0.25, 0.25);
        applyEdits();

        record(recordingTime);

        gotoEditMode();
        trim(0, 0.25);
        applyEdits();

        record(recordingTime);

        uploadSound("An edit test upload", null, true);

        assertSoundUploaded();
        assertSoundTranscoded();
    }


    /*
     * Record something, enable fading and upload. Make sure transcoding works.
     */
    public void ignore_testFadingAndUpload() throws Exception {
        record(recordingTime);
        gotoEditMode();
        assertTrue(toggleFade());

        applyEdits();

        uploadSound("A faded test upload", null, true);

        assertSoundEncoded(recordingTime * 4);
        assertSoundUploaded();
        assertSoundTranscoded();
    }


    public void ignore_testTrimAndUpload() throws Exception {
        record(10000);

        gotoEditMode();
        trim(0.25, 0.25);

        applyEdits();

        uploadSound("A trimmed test upload", null, true);

        assertSoundUploaded();
        PublicApiTrack track = assertSoundTranscoded();
        assertTrackDuration(track, 5000 + ROBO_SLEEP);
    }

    public void ignore_testTrimAndFadeAndUpload() throws Exception {
        record(10000);

        gotoEditMode();
        trim(0.25, 0.25);
        assertTrue(toggleFade());

        applyEdits();

        uploadSound("A faded + trimmed test upload", null, true);

        assertSoundEncoded(recordingTime * 4);
        assertSoundUploaded();
        PublicApiTrack track = assertSoundTranscoded();
        assertTrackDuration(track, 5000 + ROBO_SLEEP);
    }

    @Suppress
    public void ignore_testEditModesGetPersisted() {
        record(recordingTime);
        gotoEditMode();

        playbackEdit();

        solo.findElement(With.text("No Fading")).click();
        solo.findElement(With.text("Not Optimized")).click();
        solo.findElement(With.text("Save")).click();

        assertState(RecordActivity.CreateState.IDLE_PLAYBACK);


        assertTrue(solo.findElement(With.text("Fades on")).isChecked());
        assertTrue(solo.findElement(With.text("Optimized")).isChecked());

        Uri recUri = getActivity().getRecorder().getRecording().toUri();
        assertNotNull(recUri);

        // start new activity just with uri
        RecordActivity create = launchActivityWithIntent("com.soundcloud.android", RecordActivity.class, new Intent().setData(recUri));
        assertSame(RecordActivity.CreateState.IDLE_PLAYBACK, create.getState());
        Solo solo2 = new Solo(getInstrumentation(), create);
        solo2.clickOnView(create.findViewById(R.id.btn_edit));
        solo2.sleep(100);
        assertSame(RecordActivity.CreateState.EDIT, create.getState());

        assertTrue(solo2.isToggleButtonChecked("Fades on"));
        assertTrue(solo2.isToggleButtonChecked("Optimized"));
    }
}
