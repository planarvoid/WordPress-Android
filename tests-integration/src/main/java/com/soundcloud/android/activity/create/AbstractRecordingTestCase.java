package com.soundcloud.android.activity.create;

import static com.soundcloud.android.activity.create.ScCreate.CreateState.*;

import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.settings.DevSettings;
import com.soundcloud.android.audio.reader.VorbisReader;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Runner;
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
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractRecordingTestCase extends ActivityTestCase<ScCreate> {
    // longer recordings on emulator
    protected static final int RECORDING_TIME = EMULATOR ? 6000 : 2000;
    protected static final int ROBO_SLEEP = 500;

    protected LocalBroadcastManager lbm;
    protected Map<String, Intent> intents;
    protected Env env;


    final private  BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intents.put(intent.getAction(), intent);
        }
    };

    private static final long TRANSCODING_WAIT_TIME = 60 * 1000 * 3; // 3 minutes
    private static final long UPLOAD_WAIT_TIME      = 20 * 1000;
    private static final boolean FAIL_ON_TRANSCODE_TIMEOUT = false;

    public AbstractRecordingTestCase() {
        super(ScCreate.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());

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
        env = getActivity().getApp().getEnv();
        setRecordingType(null);

        super.setUp();
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
        solo.clickOnView(R.id.btn_action);
        solo.sleep(howlong);
        assertState(RECORD);
        solo.clickOnView(R.id.btn_action);
        solo.assertText(R.string.reset); // "Discard"
        assertState(IDLE_PLAYBACK);
    }

    protected void gotoEditMode() {
        solo.clickOnView(getActivity().findViewById(R.id.btn_edit));
        solo.assertText(R.string.btn_revert_to_original);
        assertState(EDIT);
    }

    protected boolean toggleFade() {
        assertState(EDIT);
        ToggleButton tb = (ToggleButton) solo.getView(R.id.toggle_fade);
        solo.clickOnView(tb);
        return tb.isChecked();
    }

    protected void playback() {
        assertState(IDLE_PLAYBACK);
        solo.clickOnView(R.id.btn_play);
        assertState(PLAYBACK);
    }

    protected void playbackEdit() {
        assertState(EDIT);
        solo.clickOnView(R.id.btn_play_edit);
        assertState(EDIT_PLAYBACK);
    }

    protected void uploadSound(@Nullable String title, @Nullable String location, boolean isPrivate) {
        assertState(IDLE_PLAYBACK);

        solo.clickOnPublish();
        solo.assertActivity(ScUpload.class);

        if (title != null) {
            solo.enterTextId(R.id.what, title);
        }

        if (location != null) {
            solo.clickOnView(R.id.where);
            solo.assertActivity(LocationPicker.class);

            solo.clickOnView(R.id.where);
            solo.enterTextId(R.id.where, location);
            solo.sendKey(Solo.ENTER);

            solo.assertActivity(ScUpload.class);
        }

        if (isPrivate) {
            solo.clickOnButtonResId(R.string.sc_upload_private);
        }

        solo.clickOnText(R.string.post);
    }

    protected void applyEdits() {
        solo.clickOnText(R.string.btn_apply);
    }

    protected void assertState(ScCreate.CreateState... state) {
        ScCreate.CreateState reached = null;
        for (ScCreate.CreateState s : state) {
            if (waitForState(s, 5000)) {
                reached = s;
                break;
            }
        }
        assertNotNull(
                "state "+ Arrays.toString(state) + " not reached, current = "+ getActivity().getState(),
                reached);
    }

    protected boolean waitForState(ScCreate.CreateState state, long timeout) {

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

    protected @Nullable Intent waitForIntent(String action, long timeout) {
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

    protected @NotNull Intent assertIntentAction(String action, long timeout) {
        Intent intent = waitForIntent(action, timeout);
        assertNotNull("did not get intent action " + action, intent);
        return intent;
    }

    protected void assertSoundEncoded(long timeout) {
        assertIntentAction(UploadService.PROCESSING_STARTED,  2000);
        assertIntentAction(UploadService.PROCESSING_SUCCESS, timeout);
    }

    protected @NotNull Recording assertSoundUploaded() {
        if (!getActivity().getRecorder().shouldEncodeWhileRecording()) {
            assertSoundEncoded(UPLOAD_WAIT_TIME * 4);
        }

        Intent intent = waitForIntent(UploadService.UPLOAD_SUCCESS, UPLOAD_WAIT_TIME);
        if (intent == null) {
            if (intents.containsKey(UploadService.TRANSFER_ERROR)) {
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

    protected @Nullable Track assertSoundTranscoded() {
        // sandbox fails sometimes, only check live system
        if (env == Env.LIVE) {
            Intent intent = waitForIntent(UploadService.TRANSCODING_SUCCESS, TRANSCODING_WAIT_TIME);

            if (intent == null) {
                if (intents.containsKey(UploadService.TRANSCODING_FAILED)) {
                    fail("transcoding failed");
                } else {
                    if (FAIL_ON_TRANSCODE_TIMEOUT) fail("transcoding timeout");
                }
                return null;
            }  else {
                Track track = intent.getParcelableExtra(UploadService.EXTRA_TRACK);
                assertNotNull("track is null", track);
                return track;
            }
        } else {
            return null;
        }
    }

    protected @Nullable File fillUpSpace(long whatsLeft) throws IOException {
        File dir = Environment.getExternalStorageDirectory();
        long currentLeft = IOUtils.getSpaceLeft(dir);
        if (currentLeft > whatsLeft) {
            long fSize = currentLeft - whatsLeft;
            final File filler = new File(dir, "filler");
            RandomAccessFile file = new RandomAccessFile(filler, "rw");
            file.setLength(fSize);
            file.seek(fSize -1);
            file.write(42);
            file.close();
            return filler;
        } else return null;
    }

    protected void setRecordingType(@Nullable String type) throws Exception {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getInstrumentation().getTargetContext());

        if (type == null) {
            prefs.edit().remove(DevSettings.DEV_RECORDING_TYPE).commit();
        } else {
            prefs.edit().putString(DevSettings.DEV_RECORDING_TYPE, type).commit();
        }
    }

    protected void assertTrackDuration(Track track, long durationInMs) {
        Log.d(getClass().getSimpleName(), "assertTrack("+track+")");
        if (track != null) {
            assertTrue("track is not finished: "+track, track.state.isFinished());
            assertTrue("track has length 0: "+track, track.duration > 0);
            assertEquals("track is not in ogg format: "+track, VorbisReader.EXTENSION, track.original_format);

            // emulator uploaded tracks are longer (samplerate mismatch)
            if (!EMULATOR) {
                assertEquals("track duration: "+track, durationInMs, track.duration, 2000);
            }
        }
    }
}