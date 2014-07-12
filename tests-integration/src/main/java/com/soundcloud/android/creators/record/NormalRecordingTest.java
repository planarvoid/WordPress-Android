package com.soundcloud.android.creators.record;


import static com.soundcloud.android.creators.record.RecordActivity.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.creators.record.RecordActivity.CreateState.IDLE_RECORD;
import static com.soundcloud.android.creators.record.RecordActivity.CreateState.RECORD;

import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.creators.upload.UploadActivity;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.preferences.DeveloperPreferences;
import com.soundcloud.android.tests.SlowTest;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.with.With;

import android.content.Intent;
import android.os.Build;
import android.test.suitebuilder.annotation.Suppress;
import android.widget.EditText;

import java.io.File;

@SlowTest
public class NormalRecordingTest extends AbstractRecordingTestCase {

    public void ignore_testRecordAndPlayback() throws Exception {
        record(recordingTime);
        playback();
        solo.sleep(recordingTime + 5000);
        assertState(IDLE_PLAYBACK);
    }

    public void ignore_testRecordMakeSureFilesGetWritten() throws Exception {
        record(recordingTime);

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

    public void ignore_testRecordAndEditRevert() throws Exception {
        record(recordingTime);
        gotoEditMode();

        solo.clickOnText(R.string.btn_revert_to_original);
        solo.assertText(R.string.dialog_revert_recording_message);
        solo.clickOnText(android.R.string.ok);

        assertState(IDLE_PLAYBACK);
    }

    public void ignore_testRecordAndEditApplyAndDelete() throws Exception {
        record(recordingTime);
        gotoEditMode();
        applyEdits();
        assertState(IDLE_PLAYBACK);

        solo.clickOnText(R.string.delete);
        solo.assertText(R.string.dialog_confirm_delete_recording_message);
        solo.clickOnText(android.R.string.ok);
        assertState(IDLE_RECORD);
    }

    public void ignore_testRecordAndDelete() throws Exception {
        record(recordingTime);
        solo.clickOnText(R.string.delete); // "Discard"
        solo.assertText(R.string.dialog_confirm_delete_recording_message); // "Are you sure you want to delete this recording?"
        solo.clickOnText(android.R.string.ok);
        assertState(IDLE_RECORD);
    }

    public void ignore_testRecordAndUpload() throws Exception {
        record(recordingTime);

        uploadSound("A test upload", null, true);

        assertSoundUploaded();
        PublicApiTrack track = assertSoundTranscoded();

        if (track != null) {
            assertEquals("A test upload", track.title);
            assertFalse("track is public", track.isPublic());

            assertTrackDuration(track, recordingTime + ROBO_SLEEP);
        }

    }

    public void ignore_testRecordAndUploadWithLocation() throws Exception {
        record(recordingTime);

        final String location = "Model "+Build.MODEL;
        uploadSound("A test upload", location, true);

        assertSoundUploaded();
        PublicApiTrack track = assertSoundTranscoded();
        if (track != null) {
            assertEquals("A test upload at "+location, track.title);
        }
    }

    public void ignore_testRecordAndUploadRaw() throws Exception {
        setRecordingType(DeveloperPreferences.DEV_RECORDING_TYPE_RAW);
        record(recordingTime);

        assertTrue("raw file does not exist", getActivity().getRecorder().getRecording().getFile().exists());
        assertFalse("encoded file exists", getActivity().getRecorder().getRecording().getEncodedFile().exists());

        uploadSound("A raw test upload", null, true);

        assertSoundUploaded();
        assertSoundTranscoded();
    }

    public void ignore_testRecordAndUploadThenRecordAnotherSound() throws Exception {
        record(recordingTime);

        solo.clickOnText(R.string.btn_publish);

        solo.clickOnText(R.string.record_another_sound);

        assertState(IDLE_RECORD); // should be read to record a new track
    }

    public void ignore_testRecordAndUploadThenGoBack() throws Exception {
        record(recordingTime);

        solo.clickOnText(R.string.btn_publish);

        solo.goBack();

        // softkeyboard gets shown on some versions of android
        if (solo.getCurrentActivity() instanceof UploadActivity) solo.goBack();


        assertState(IDLE_PLAYBACK); // should be old recording
    }

    @SlowTest
    public void testRecordAndRunningOutOfStorageSpace() throws Exception {
        if (!applicationProperties.isRunningOnEmulator()) return;

        File filler = fillUpSpace(1024*1024);
        try {
            assertState(IDLE_RECORD, IDLE_PLAYBACK);
            long remaining = getActivity().getRecorder().timeRemaining();
            // countdown starts for last 5 minutes of recording time
            assertTrue("remaining time over 5 mins: "+remaining, remaining < 300);

            solo.findElement(With.id(R.id.btn_action)).click();
            solo.sleep(1000);

            while (getActivity().getRecorder().timeRemaining() > 10) {
                assertState(RECORD);
                solo.sleep(100);
                solo.findElement(With.id(R.id.chronometer)).getText().matches("(?:\\d+|One) (?:minute|second)s? available");
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


    public void ignore_testRecordAndAppendAndUpload() throws Exception {
        record(recordingTime);

        solo.sleep(1000);

        record(recordingTime);

        uploadSound("An appended playable", null, true);

        assertSoundUploaded();
        PublicApiTrack track = assertSoundTranscoded();
        assertTrackDuration(track, 2 * (recordingTime + ROBO_SLEEP));
    }

    public void ignore_testRecordRawAndAppendAndUpload() throws Exception {
        setRecordingType(DeveloperPreferences.DEV_RECORDING_TYPE_RAW);

        record(recordingTime);
        solo.sleep(1000);
        record(recordingTime);
        solo.sleep(1000);
        record(recordingTime);

        uploadSound("An appended raw playable", null, true);

        assertSoundEncoded(recordingTime * 3 * 4);
        assertSoundUploaded();
        PublicApiTrack track = assertSoundTranscoded();
        assertTrackDuration(track, 3 * (recordingTime + ROBO_SLEEP));
    }

    public void ignore_testShouldRegenerateWaveFormIfItGetsLost() throws Exception {
        record(recordingTime);
        solo.sleep(1000);

        Recording r = getActivity().getRecorder().getRecording();

        solo.finishOpenedActivities();
        getActivity().getRecorder().reset();

        File ampFile = r.getAmplitudeFile();

        assertTrue(ampFile.exists());
        assertTrue(ampFile.delete());

        launchActivityWithIntent("com.soundcloud.android", RecordActivity.class, new Intent().putExtra(Recording.EXTRA, r));

        solo.sleep(4000);

        assertTrue(ampFile.exists());
    }

    public void ignore_testDeleteWavFileAndPlayback() throws Exception {
        record(recordingTime);
        solo.sleep(1000);
        Recording r = getActivity().getRecorder().getRecording();
        File wavFile = r.getFile();

        assertTrue(wavFile.exists());
        assertTrue(wavFile.delete());

        RecordActivity create = reloadRecording(r);
        setActivity(create);

        playback();
    }

    public void ignore_testDeleteWavFileAndUpload() throws Exception {
        // test only makes sense if we have an ogg file + wav file
        if (!getActivity().getRecorder().shouldEncodeWhileRecording()) return;

        record(recordingTime);
        solo.sleep(1000);

        // create a faded and trimmed recording to test re-encoding of ogg file
        gotoEditMode();
        trim(0.1, 0.1);
        assertTrue(toggleFade());
        applyEdits();

        Recording r = getActivity().getRecorder().getRecording();

        solo.clickOnText(R.string.btn_publish);

        long tstamp = System.currentTimeMillis();

        final String title ="testDeleteWavFileAndUpload-"+tstamp;
        // give it a title
        solo.findElement(With.id(R.id.what)).typeText(title);
        solo.goBack();

        // doesn't exist any longer
        //testDriver.clickOnView(R.id.action_bar_local_recordings);

        // delete wav file
        File wavFile = r.getFile();
        assertTrue(wavFile.exists());
        assertTrue(wavFile.delete());

        solo.clickOnText(title);

        uploadSound(null, null, true);

        assertSoundUploaded();
        PublicApiTrack t = assertSoundTranscoded();

        if (t != null) {
            assertEquals(title, t.title);
        }
    }

    public void ignore_testShouldAutoSaveRecordingAndNavigateToYourSounds() throws Exception {
        record(recordingTime);
        solo.assertText(R.string.rec_your_sound_is_saved_locally_at);
        solo.findElement(With.id(R.id.home)).click();
        solo.clickOnText(TestUser.defaultUser.getUsername());
    }

    public void ignore_testShouldOnlyDisplayedSavedLocallyMessageOnce() throws Exception {
        record(recordingTime);
        solo.assertText(R.string.rec_your_sound_is_saved_locally_at);
        solo.sleep(500);
        solo.findElement(With.id(R.id.btn_action)).click();
        solo.sleep(1000);
        solo.findElement(With.id(R.id.btn_action)).click();
        solo.findElements(With.text(solo.getString(R.string.rec_your_sound_is_saved_locally_at))).isEmpty();
    }

    @Suppress
    public void ignore_testRecordAndLoadAndAppend() throws Exception {
        record(recordingTime);

        solo.clickOnText(R.string.btn_publish);

        long id = System.currentTimeMillis();
        final String name = "A test upload " + id;
        solo.findElements(With.className(EditText.class)).get(0).typeText(name);

        setActivity(reloadRecording(getActivity().getRecorder().getRecording()));

        record(recordingTime);
    }
}
