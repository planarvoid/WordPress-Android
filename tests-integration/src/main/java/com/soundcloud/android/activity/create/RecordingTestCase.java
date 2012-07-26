package com.soundcloud.android.activity.create;

import static com.soundcloud.android.activity.create.ScCreate.CreateState.*;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.settings.DevSettings;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Runner;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Env;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class RecordingTestCase extends ActivityTestCase<ScCreate> {
    // longer recordings on emulator
    protected static final int RECORDING_TIME = EMULATOR ? 6000 : 2000;

    protected LocalBroadcastManager lbm;
    protected List<Intent> intents;
    protected Env env;


    final private  BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intents.add(intent);
        }
    };

    public RecordingTestCase() {
        super(ScCreate.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());

        intents = Collections.synchronizedList(new ArrayList<Intent>());
        lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(receiver, UploadService.getIntentFilter());
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getActivity().reset();
            }
        });

        Runner.checkFreeSpace();
        env = getActivity().getApp().getEnv();
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

    protected boolean waitForIntent(String action, long timeout) {
        final long startTime = SystemClock.uptimeMillis();
        final long endTime = startTime + timeout;
        while (SystemClock.uptimeMillis() < endTime) {
            solo.sleep(100);
            for (Intent intent : new ArrayList<Intent>(intents)) {
                if (action.equals(intent.getAction())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void assertIntentAction(String action, long timeout) {
        assertTrue("did not get intent action "+action, waitForIntent(action, timeout));
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
}