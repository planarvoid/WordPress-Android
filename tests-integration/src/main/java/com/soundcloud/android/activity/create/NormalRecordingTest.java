package com.soundcloud.android.activity.create;


import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.IDLE_RECORD;
import static com.soundcloud.android.activity.create.ScCreate.CreateState.RECORD;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.activity.settings.DevSettings;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.upload.UploadService;

import android.content.Intent;
import android.os.Build;
import android.test.suitebuilder.annotation.Suppress;
import android.widget.EditText;

import java.io.File;

public class NormalRecordingTest extends AbstractRecordingTestCase {

    public void testRecordAndPlayback() throws Exception {
        record(RECORDING_TIME);
        playback();
        solo.sleep(RECORDING_TIME + 500);
        assertState(IDLE_PLAYBACK);
    }

    public void testRecordMakeSureFilesGetWritten() throws Exception {
        record(RECORDING_TIME);
        Recording r = getActivity().getRecorder().getRecording();

        File raw = r.getFile();
        File encoded = r.getEncodedFile();

        assertTrue(raw.exists());
        assertTrue(encoded.exists());

        assertTrue(raw.length() > 0);
        assertTrue("encoded length "+encoded.length(), encoded.length() > 0);
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

            if (!EMULATOR) {
                assertEquals("track duration is off", RECORDING_TIME, track.duration, 2000);
            }
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
        try {
            record(RECORDING_TIME);

            assertTrue("raw file does not exist", getActivity().getRecorder().getRecording().getFile().exists());
            assertFalse("encoded file exists", getActivity().getRecorder().getRecording().getEncodedFile().exists());

            solo.clickOnPublish();
            solo.assertActivity(ScUpload.class);

            solo.enterText(0, "A test upload");
            solo.clickOnButtonResId(R.string.sc_upload_private);
            solo.clickOnText(R.string.post);

            assertIntentAction(UploadService.PROCESSING_STARTED,  2000);
            assertIntentAction(UploadService.PROCESSING_PROGRESS, 5000);
            assertIntentAction(UploadService.PROCESSING_SUCCESS, 20000);

            assertSoundUploaded();
            assertSoundTranscoded();
            solo.assertActivityFinished();
        } finally {
            setRecordingType(null);
        }
    }

    public void testRecordAndSharePrivatelyToEmailAddress() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        solo.enterText(0, "A test upload");
        solo.clickOnButtonResId(R.string.sc_upload_private);

        solo.clickOnText(R.string.sc_upload_only_you);

        solo.assertActivity(EmailPicker.class);

        solo.enterText(0, "recipient@example.com");
        solo.clickOnOK();

        solo.assertActivity(ScUpload.class);

        solo.assertNoText(R.string.sc_upload_only_you);
        solo.assertText("recipient@example.com");
        solo.clickOnText("recipient@example.com");

        solo.assertActivity(EmailPicker.class);
        solo.assertText("recipient@example.com");

        solo.clickOnButtonResId(R.string.email_picker_clear);
        solo.clickOnOK();

        solo.assertActivity(ScUpload.class);
        solo.assertText(R.string.sc_upload_only_you);
    }

    public void testRecordAndSharePrivatelyToMultipleEmailAddresses() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        solo.enterText(0, "A test upload");
        solo.clickOnButtonResId(R.string.sc_upload_private);

        solo.clickOnText(R.string.sc_upload_only_you);

        solo.assertActivity(EmailPicker.class);

        solo.enterText(0, "recipient@example.com, another@example.com, foo@example.com");
        solo.clickOnOK();

        solo.assertActivity(ScUpload.class);

        solo.assertNoText(R.string.sc_upload_only_you);
        solo.assertText("recipient@example.com");
        solo.assertText("another@example.com");
        solo.assertText("foo@example.com");

        solo.clickOnText("another@example.com");

        solo.assertActivity(EmailPicker.class);
        solo.assertText("recipient@example.com");
        solo.assertText("another@example.com");
        solo.assertText("foo@example.com");

        EditText email = (EditText) solo.getView(R.id.email);
        assertEquals("cursor is not positioned correctly",
                "recipient@example.com".length() +
                "another@example.com".length() + 2,

                email.getSelectionStart() );
        solo.clickOnOK();
        solo.assertActivity(ScUpload.class);
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

        solo.assertActivity(ScCreate.class);
        assertState(IDLE_PLAYBACK); // should be old recording
    }


    public void testRecordAndRunningOutOfStorageSpace() throws Exception {
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

    @Suppress
    public void testRecordAndLoadAndAppend() throws Exception {
        record(RECORDING_TIME);

        solo.clickOnPublish();

        long id = System.currentTimeMillis();
        final String name = "A test upload " + id;
        solo.enterText(0, name);

        solo.assertActivity(ScUpload.class);

        solo.finishOpenedActivities();

        Main main = launchActivityWithIntent("com.soundcloud.android",
            Main.class, new Intent().putExtra(Main.TAB_TAG, Main.Tab.PROFILE.tag));


        solo.clickOnText(name);

        solo.sleep(500);

//        solo.assertActivity(ScCreate.class);

        record(RECORDING_TIME);

        solo.sleep(5000);

    }
}
