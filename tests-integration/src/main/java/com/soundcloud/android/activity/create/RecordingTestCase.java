package com.soundcloud.android.activity.create;

import static com.soundcloud.android.activity.create.ScCreate.CreateState.*;

import com.soundcloud.android.R;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Runner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class RecordingTestCase extends ActivityTestCase<ScCreate> {
    // longer recordings on emulator
    protected static final int RECORDING_TIME = EMULATOR ? 6000 : 2000;

    protected LocalBroadcastManager lbm;
    protected List<Intent> intents;

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
            for (Intent intent : intents) {
                if (action.equals(intent.getAction())) {
                    return true;
                }
            }
        }
        return false;
    }
}