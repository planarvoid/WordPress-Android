package com.soundcloud.android.activity.create;


import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_RECORD;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.RECORD;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.activity.settings.DevSettings;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.record.SoundRecorder;

import android.content.Intent;
import android.os.Build;
import android.test.suitebuilder.annotation.Suppress;

import java.io.File;

public class NormalRecordingTest extends AbstractRecordingTestCase {

    public void testRecordAndPlayback() throws Exception {
        record(RECORDING_TIME);
        playback();
        solo.sleep(RECORDING_TIME + 5000);
        assertState(IDLE_PLAYBACK);
    }

    public void testRecordMakeSureFilesGetWritten() throws Exception {
        record(RECORDING_TIME);

        SoundRecorder recorder = getActivity().getRecorder();

        Recording r = recorder.getRecording();

        File raw = r.getFile();
        assertTrue(raw.exists());
        assertTrue(raw.length() > 0);

        if (recorder.shouldEncodeWhileRecording())  {
            File encoded = r.getEncodedFile();
            assertTrue(encoded.exists());
            assertTrue("encoded length " + encoded.length(), encoded.length() > 0);
        }
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
        applyEdits();
        assertState(IDLE_PLAYBACK);

        solo.clickOnText(R.string.delete);
        solo.assertText(R.string.dialog_confirm_delete_recording_message);
        solo.clickOnOK();
        solo.sleep(1000);
        solo.assertActivityFinished();
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

        uploadSound("A test upload", null, true);

        assertSoundUploaded();
        Track track = assertSoundTranscoded();

        if (track != null) {
            assertEquals("A test upload", track.title);
            assertFalse("track is public", track.isPublic());

            assertTrackDuration(track, RECORDING_TIME + ROBO_SLEEP);
        }

        solo.assertActivityFinished();
    }

    public void testRecordAndUploadWithLocation() throws Exception {
        record(RECORDING_TIME);

        final String location = "Model "+Build.MODEL;
        uploadSound("A test upload", location, true);

        assertSoundUploaded();
        Track track = assertSoundTranscoded();
        if (track != null) {
            assertEquals("A test upload at "+location, track.title);
        }
        solo.assertActivityFinished();
    }

    public void testRecordAndUploadRaw() throws Exception {
        setRecordingType(DevSettings.DEV_RECORDING_TYPE_RAW);
        record(RECORDING_TIME);

        assertTrue("raw file does not exist", getActivity().getRecorder().getRecording().getFile().exists());
        assertFalse("encoded file exists", getActivity().getRecorder().getRecording().getEncodedFile().exists());

        uploadSound("A raw test upload", null, true);

        assertSoundUploaded();
        assertSoundTranscoded();
        solo.assertActivityFinished();
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

        // softkeyboard gets shown on some versions of android
        if (solo.getCurrentActivity() instanceof  ScUpload) solo.goBack();

        solo.assertActivity(ScCreate.class);

        assertState(IDLE_PLAYBACK); // should be old recording
    }

    public void testRecordAndRunningOutOfStorageSpace() throws Exception {
        if (!EMULATOR) return;

        File filler = fillUpSpace(1024*1024);
        try {
            assertState(IDLE_RECORD, IDLE_PLAYBACK);
            long remaining = getActivity().getRecorder().timeRemaining();
            // countdown starts for last 5 minutes of recording time
            assertTrue("remaining time over 5 mins: "+remaining, remaining < 300);

            solo.clickOnView(R.id.btn_action);
            solo.sleep(1000);

            while (getActivity().getRecorder().timeRemaining() > 10) {
                assertState(RECORD);
                solo.sleep(100);
                solo.assertVisibleText("(?:\\d+|One) (?:minute|second)s? available", 100);
            }

            solo.assertText(R.string.record_storage_is_full);
            assertEquals(0, getActivity().getRecorder().timeRemaining());
            // out of space, assert player paused
            assertState(IDLE_PLAYBACK);
        } finally {
            if (filler != null) {
                filler.delete();
            }
        }
    }


    public void testRecordAndAppendAndUpload() throws Exception {
        record(RECORDING_TIME);

        solo.sleep(1000);

        record(RECORDING_TIME);

        uploadSound("An appended sound", null, true);

        assertSoundUploaded();
        Track track = assertSoundTranscoded();
        assertTrackDuration(track, 2 * (RECORDING_TIME + ROBO_SLEEP));
    }

    public void testRecordRawAndAppendAndUpload() throws Exception {
        setRecordingType(DevSettings.DEV_RECORDING_TYPE_RAW);

        record(RECORDING_TIME);
        solo.sleep(1000);
        record(RECORDING_TIME);
        solo.sleep(1000);
        record(RECORDING_TIME);

        uploadSound("An appended raw sound", null, true);

        assertSoundEncoded(RECORDING_TIME * 3 * 4);
        assertSoundUploaded();
        Track track = assertSoundTranscoded();
        assertTrackDuration(track, 3 * (RECORDING_TIME + ROBO_SLEEP));
    }

    public void testShouldRegenerateWaveFormIfItGetsLost() throws Exception {
        record(RECORDING_TIME);
        solo.sleep(1000);

        Recording r = getActivity().getRecorder().getRecording();

        solo.finishOpenedActivities();
        getActivity().getRecorder().reset();

        File ampFile = r.getAmplitudeFile();

        assertTrue(ampFile.exists());
        assertTrue(ampFile.delete());

        launchActivityWithIntent("com.soundcloud.android", ScCreate.class, new Intent().putExtra(Recording.EXTRA, r));

        solo.sleep(4000);

        assertTrue(ampFile.exists());
    }

    public void testDeleteWavFileAndPlayback() throws Exception {
        record(RECORDING_TIME);
        solo.sleep(1000);
        Recording r = getActivity().getRecorder().getRecording();
        File wavFile = r.getFile();

        assertTrue(wavFile.exists());
        assertTrue(wavFile.delete());

        ScCreate create = reloadRecording(r);
        setActivity(create);

        playback();
    }

    public void testDeleteWavFileAndUpload() throws Exception {
        record(RECORDING_TIME);
        solo.sleep(1000);

        // create a faded and trimmed recording to test re-encoding of ogg file
        gotoEditMode();
        trim(0.1, 0.1);
        assertTrue(toggleFade());
        applyEdits();

        Recording r = getActivity().getRecorder().getRecording();

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        long tstamp = System.currentTimeMillis();

        final String title ="testDeleteWavFileAndUpload-"+tstamp;
        // give it a title
        solo.enterTextId(R.id.what, title);
        solo.goBack();

        solo.clickOnView(R.id.btn_you);

        // delete wav file
        File wavFile = r.getFile();
        assertTrue(wavFile.exists());
        assertTrue(wavFile.delete());

        solo.clickOnText(title);

        solo.assertActivity(ScCreate.class);
        uploadSound(null, null, true);

        assertSoundUploaded();
        Track t = assertSoundTranscoded();

        if (t != null) {
            assertEquals(title, t.title);
        }
    }

    public void testShouldAutoSaveRecordingAndNavigateToYourSounds() throws Exception {
        record(RECORDING_TIME);
        solo.assertText(R.string.rec_your_sound_is_saved_locally_at);
        solo.clickOnView(R.id.btn_you);
        solo.assertActivity(Main.class);
    }

    public void testShouldOnlyDisplayedSavedLocallyMessageOnce() throws Exception {
        record(RECORDING_TIME);
        solo.assertText(R.string.rec_your_sound_is_saved_locally_at);
        solo.sleep(500);
        solo.clickOnView(R.id.btn_action);
        solo.sleep(1000);
        solo.clickOnView(R.id.btn_action);
        solo.assertNoText(R.string.rec_your_sound_is_saved_locally_at);
    }

    @Suppress
    public void testRecordAndLoadAndAppend() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnPublish();

        long id = System.currentTimeMillis();
        final String name = "A test upload " + id;
        solo.enterText(0, name);

        solo.assertActivity(ScUpload.class);

        setActivity(reloadRecording(getActivity().getRecorder().getRecording()));

        record(RECORDING_TIME);

    }
}
