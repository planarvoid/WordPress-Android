package com.soundcloud.android.activity.create;


import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_RECORD;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.RECORD;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.You;
import com.soundcloud.android.activity.settings.DevSettings;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.SlowTest;

import android.content.Intent;
import android.os.Build;
import android.test.suitebuilder.annotation.Suppress;

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
        solo.clickOnOK();

        assertState(IDLE_PLAYBACK);
    }

    public void ignore_testRecordAndEditApplyAndDelete() throws Exception {
        record(recordingTime);
        gotoEditMode();
        applyEdits();
        assertState(IDLE_PLAYBACK);

        solo.clickOnText(R.string.delete);
        solo.assertText(R.string.dialog_confirm_delete_recording_message);
        solo.clickOnOK();
        assertState(IDLE_RECORD);
    }

    public void ignore_testRecordAndDelete() throws Exception {
        record(recordingTime);
        solo.clickOnText(R.string.delete); // "Discard"
        solo.assertText(R.string.dialog_confirm_delete_recording_message); // "Are you sure you want to delete this recording?"
        solo.clickOnOK();
        assertState(IDLE_RECORD);
    }

    public void ignore_testRecordAndUpload() throws Exception {
        record(recordingTime);

        uploadSound("A test upload", null, true);

        assertSoundUploaded();
        Track track = assertSoundTranscoded();

        if (track != null) {
            assertEquals("A test upload", track.title);
            assertFalse("track is public", track.isPublic());

            assertTrackDuration(track, recordingTime + ROBO_SLEEP);
        }

        solo.assertActivityFinished();
    }

    public void ignore_testRecordAndUploadWithLocation() throws Exception {
        record(recordingTime);

        final String location = "Model "+Build.MODEL;
        uploadSound("A test upload", location, true);

        assertSoundUploaded();
        Track track = assertSoundTranscoded();
        if (track != null) {
            assertEquals("A test upload at "+location, track.title);
        }
        solo.assertActivityFinished();
    }

    public void ignore_testRecordAndUploadRaw() throws Exception {
        setRecordingType(DevSettings.DEV_RECORDING_TYPE_RAW);
        record(recordingTime);

        assertTrue("raw file does not exist", getActivity().getRecorder().getRecording().getFile().exists());
        assertFalse("encoded file exists", getActivity().getRecorder().getRecording().getEncodedFile().exists());

        uploadSound("A raw test upload", null, true);

        assertSoundUploaded();
        assertSoundTranscoded();
        solo.assertActivityFinished();
    }

    public void ignore_testRecordAndUploadThenRecordAnotherSound() throws Exception {
        record(recordingTime);

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        solo.clickOnText(R.string.record_another_sound);

        solo.assertActivity(ScCreate.class);
        assertState(IDLE_RECORD); // should be read to record a new track
    }

    public void ignore_testRecordAndUploadThenGoBack() throws Exception {
        record(recordingTime);

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        solo.goBack();

        // softkeyboard gets shown on some versions of android
        if (solo.getCurrentActivity() instanceof  ScUpload) solo.goBack();

        solo.assertActivity(ScCreate.class);

        assertState(IDLE_PLAYBACK); // should be old recording
    }

    @SlowTest
    public void ignore_testRecordAndRunningOutOfStorageSpace() throws Exception {
        if (!applicationProperties.isRunningOnEmulator()) return;

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


    public void ignore_testRecordAndAppendAndUpload() throws Exception {
        record(recordingTime);

        solo.sleep(1000);

        record(recordingTime);

        uploadSound("An appended playable", null, true);

        assertSoundUploaded();
        Track track = assertSoundTranscoded();
        assertTrackDuration(track, 2 * (recordingTime + ROBO_SLEEP));
    }

    public void ignore_testRecordRawAndAppendAndUpload() throws Exception {
        setRecordingType(DevSettings.DEV_RECORDING_TYPE_RAW);

        record(recordingTime);
        solo.sleep(1000);
        record(recordingTime);
        solo.sleep(1000);
        record(recordingTime);

        uploadSound("An appended raw playable", null, true);

        assertSoundEncoded(recordingTime * 3 * 4);
        assertSoundUploaded();
        Track track = assertSoundTranscoded();
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

        launchActivityWithIntent("com.soundcloud.android", ScCreate.class, new Intent().putExtra(Recording.EXTRA, r));

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

        ScCreate create = reloadRecording(r);
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

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        long tstamp = System.currentTimeMillis();

        final String title ="testDeleteWavFileAndUpload-"+tstamp;
        // give it a title
        solo.enterTextId(R.id.what, title);
        solo.goBack();

        // doesn't exist any longer
        //solo.clickOnView(R.id.action_bar_local_recordings);

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

    public void ignore_testShouldAutoSaveRecordingAndNavigateToYourSounds() throws Exception {
        record(recordingTime);
        solo.assertText(R.string.rec_your_sound_is_saved_locally_at);
        solo.clickOnView(R.id.home);
        solo.clickOnText(AccountAssistant.USERNAME);
        solo.assertActivity(You.class);
    }

    public void ignore_testShouldOnlyDisplayedSavedLocallyMessageOnce() throws Exception {
        record(recordingTime);
        solo.assertText(R.string.rec_your_sound_is_saved_locally_at);
        solo.sleep(500);
        solo.clickOnView(R.id.btn_action);
        solo.sleep(1000);
        solo.clickOnView(R.id.btn_action);
        solo.assertNoText(R.string.rec_your_sound_is_saved_locally_at);
    }

    @Suppress
    public void ignore_testRecordAndLoadAndAppend() throws Exception {
        record(recordingTime);

        solo.clickOnPublish();

        long id = System.currentTimeMillis();
        final String name = "A test upload " + id;
        solo.enterText(0, name);

        solo.assertActivity(ScUpload.class);

        setActivity(reloadRecording(getActivity().getRecorder().getRecording()));

        record(recordingTime);
    }
}
