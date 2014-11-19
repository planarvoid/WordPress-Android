package com.soundcloud.android.creators.record;

import static com.soundcloud.android.creators.record.RecordActivity.CreateState.EDIT;
import static com.soundcloud.android.creators.record.RecordActivity.CreateState.EDIT_PLAYBACK;
import static com.soundcloud.android.creators.record.RecordActivity.CreateState.IDLE_PLAYBACK;
import static com.soundcloud.android.creators.record.RecordActivity.CreateState.IDLE_RECORD;
import static com.soundcloud.android.creators.record.RecordActivity.CreateState.PLAYBACK;
import static com.soundcloud.android.creators.record.RecordActivity.CreateState.RECORD;

import com.robotium.solo.Solo;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.reader.VorbisReader;
import com.soundcloud.android.creators.upload.UploadService;
import com.soundcloud.android.settings.DeveloperSettings;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Runner;
import com.soundcloud.android.tests.viewelements.EditTextElement;
import com.soundcloud.android.tests.viewelements.ViewElement;
import com.soundcloud.android.tests.with.With;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Env;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractRecordingTestCase extends ActivityTestCase<RecordActivity> {
    // longer recordings on emulator
    protected int recordingTime;
    protected static final int ROBO_SLEEP = 500;

    protected LocalBroadcastManager lbm;
    protected Map<String, Intent> intents;
    protected Env env;


    final private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intents.put(intent.getAction(), intent);
        }
    };

    private static final long TRANSCODING_WAIT_TIME = 60 * 1000 * 3; // 3 minutes
    private static final long UPLOAD_WAIT_TIME = 20 * 1000;
    private static final boolean FAIL_ON_TRANSCODE_TIMEOUT = false;

    // somehow the api sometimes reports 0 as length, after successful transcoding */
    private static final boolean FAIL_ON_ZERO_DURATION = true;

    public AbstractRecordingTestCase() {
        super(RecordActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        super.setUp();
        recordingTime = applicationProperties.isRunningOnEmulator() ? 6000 : 2000;
        intents = Collections.synchronizedMap(new LinkedHashMap<String, Intent>());
        lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(receiver, UploadService.getIntentFilter());

        IOUtils.deleteDir(SoundRecorder.RECORD_DIR);
        IOUtils.mkdirs(SoundRecorder.RECORD_DIR);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getActivity().reset();
            }
        });

        Runner.checkFreeSpace();
        //Do we every not hit live?
        env = Env.LIVE;
        setRecordingType(null);

    }

    @Override
    public void tearDown() throws Exception {
        lbm.unregisterReceiver(receiver);
        super.tearDown();
    }

    protected void record(int howlong) {
        record(howlong, solo.getString(R.string.record_instructions));
    }

    protected void record(int howlong, String text) {
        solo.assertText(text);
        assertState(IDLE_RECORD, IDLE_PLAYBACK);
        solo.findElement(With.id(R.id.btn_action)).click();
        solo.sleep(howlong);
        assertState(RECORD);
        solo.findElement(With.id(R.id.btn_action)).click();
        solo.assertText(R.string.reset); // "Discard"
        assertState(IDLE_PLAYBACK);
    }

    protected void gotoEditMode() {
        solo.findElement(With.id(R.id.btn_edit)).click();
        solo.assertText(R.string.btn_revert_to_original);
        assertState(EDIT);
    }

    protected boolean toggleFade() {
        assertState(EDIT);

        ViewElement tb = solo.findElement(With.id(R.id.toggle_fade));
        tb.click();

        return tb.isChecked();
    }

    protected void playback() {
        assertState(IDLE_PLAYBACK);
        solo.findElement(With.id(R.id.btn_play)).click();
        assertState(PLAYBACK);
    }

    protected void playbackEdit() {
        assertState(EDIT);
        solo.findElement(With.id(R.id.btn_play_edit)).click();
        assertState(EDIT_PLAYBACK);
    }

    protected void uploadSound(@Nullable String title, @Nullable String location, boolean isPrivate) {
        assertState(IDLE_PLAYBACK);

        solo.clickOnText(R.string.btn_publish);

        if (title != null) {
            new EditTextElement(solo.findElement(With.id(R.id.what))).typeText(title);
        }

        if (location != null) {
            solo.findElement(With.id(R.id.where)).click();

            solo.findElement(With.id(R.id.where)).click();
            new EditTextElement(solo.findElement(With.id(R.id.what))).typeText(location);
            solo.sendKey(Solo.ENTER);

        }

        if (isPrivate) {
            solo.clickOnButtonWithText(R.string.sc_upload_private);
        }

        solo.clickOnText(R.string.post);
    }

    protected void applyEdits() {
        solo.clickOnText(R.string.btn_apply);
    }

    protected void assertState(RecordActivity.CreateState... state) {
        RecordActivity.CreateState reached = null;
        for (RecordActivity.CreateState s : state) {
            if (waitForState(s, 5000)) {
                reached = s;
                break;
            }
        }
        assertNotNull(
                "state " + Arrays.toString(state) + " not reached, current = " + getActivity().getState(),
                reached);
    }

    protected boolean waitForState(RecordActivity.CreateState state, long timeout) {

        final long startTime = SystemClock.uptimeMillis();
        final long endTime = startTime + timeout;
        while (SystemClock.uptimeMillis() < endTime) {
            solo.sleep(100);
            if (getActivity().getState() == state) {
                return true;
            }
        }
        return false;
    }

    protected
    @Nullable
    Intent waitForIntent(String action, long timeout) {
        final long startTime = SystemClock.uptimeMillis();
        final long endTime = startTime + timeout;
        while (SystemClock.uptimeMillis() < endTime) {
            solo.sleep(100);
            if (intents.containsKey(action)) {
                return intents.get(action);
            }
        }
        return null;
    }

    protected
    @NotNull
    Intent assertIntentAction(String action, long timeout) {
        Intent intent = waitForIntent(action, timeout);
        assertNotNull("did not get intent action " + action, intent);
        return intent;
    }

    protected void assertSoundEncoded(long timeout) {
        assertIntentAction(UploadService.PROCESSING_STARTED, 2000);
        assertIntentAction(UploadService.PROCESSING_SUCCESS, timeout);
    }

    protected
    @NotNull
    Recording assertSoundUploaded() {
        if (!getActivity().getRecorder().shouldEncodeWhileRecording()) {
            assertSoundEncoded(UPLOAD_WAIT_TIME * 4);
        }

        Intent intent = waitForIntent(UploadService.UPLOAD_SUCCESS, UPLOAD_WAIT_TIME);
        if (intent == null) {
            if (intents.containsKey(UploadService.PROCESSING_ERROR)) {
                fail("processing error");
            } else if (intents.containsKey(UploadService.TRANSFER_ERROR)) {
                fail("transfer error");
            } else {
                fail("upload timeout");
            }
            return null;
        } else {
            Recording recording = intent.getParcelableExtra(UploadService.EXTRA_RECORDING);
            assertNotNull("recording is null", recording);
            return recording;
        }
    }

    protected
    @Nullable
    PublicApiTrack assertSoundTranscoded() {
        // sandbox fails sometimes, only check live system
        if (env == Env.LIVE) {
            Intent intent = waitForIntent(UploadService.TRANSCODING_SUCCESS, TRANSCODING_WAIT_TIME);

            if (intent == null) {
                if (intents.containsKey(UploadService.TRANSCODING_FAILED)) {
                    fail("transcoding failed");
                } else {
                    if (FAIL_ON_TRANSCODE_TIMEOUT) {
                        fail("transcoding timeout");
                    }
                }
                return null;
            } else {
                PublicApiTrack track = intent.getParcelableExtra(PublicApiTrack.EXTRA);
                assertNotNull("track is null", track);
                return track;
            }
        } else {
            return null;
        }
    }

    protected
    @Nullable
    File fillUpSpace(long whatsLeft) throws IOException {
        File dir = Environment.getExternalStorageDirectory();
        long currentLeft = IOUtils.getSpaceLeft(dir);
        if (currentLeft > whatsLeft) {
            long fSize = currentLeft - whatsLeft;
            final File filler = new File(dir, "filler");
            RandomAccessFile file = new RandomAccessFile(filler, "rw");
            file.setLength(fSize);
            file.seek(fSize - 1);
            file.write(42);
            file.close();
            return filler;
        } else {
            return null;
        }
    }

    protected void setRecordingType(@Nullable String type) throws Exception {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getInstrumentation().getTargetContext());

        if (type == null) {
            prefs.edit().remove(DeveloperSettings.DEV_RECORDING_TYPE).commit();
        } else {
            prefs.edit().putString(DeveloperSettings.DEV_RECORDING_TYPE, type).commit();
        }
    }

    protected void assertTrackDuration(PublicApiTrack track, long durationInMs) {
        Log.d(getClass().getSimpleName(), "assertTrack(" + track + ")");
        if (track != null) {
            assertTrue("track is not finished: " + track, track.isFinished());
            assertEquals("track is not in ogg format: " + track, VorbisReader.EXTENSION, track.original_format);

            // emulator uploaded tracks are longer (samplerate mismatch)
            if (!applicationProperties.isRunningOnEmulator()) {
                assertEquals("track duration: " + track, durationInMs, track.duration, 2000);
            }

            if (FAIL_ON_ZERO_DURATION) {
                assertTrue("track has length 0: " + track, track.duration > 0);
            }
        }
    }

    protected RecordActivity reloadRecording(Recording r) {
        solo.finishOpenedActivities();
        getActivity().getRecorder().reset();
        return launchActivityWithIntent("com.soundcloud.android", RecordActivity.class, new Intent().setData(r.toUri()));
    }

    protected void trim(double left, double right) {
        assertState(EDIT);
        ViewElement leftTrim, rightTrim;
        leftTrim = solo.findElements(With.className(TrimHandleView.class)).get(0);
        rightTrim = solo.findElements(With.className(TrimHandleView.class)).get(1);
        int width = solo.getScreenWidth();

        if (left > 0) {
            leftTrim.dragHorizontally((int) (width * left), 5);
        }
        if (right > 0) {
            rightTrim.dragHorizontally(-(int) (width * right), 5);
        }
    }
}
