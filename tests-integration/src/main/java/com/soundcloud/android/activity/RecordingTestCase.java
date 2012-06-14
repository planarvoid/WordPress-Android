package com.soundcloud.android.activity;

import static com.soundcloud.android.activity.ScCreate.CreateState.IDLE_PLAYBACK;

import com.soundcloud.android.R;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.test.ActivityInstrumentationTestCase2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class RecordingTestCase extends ActivityInstrumentationTestCase2<ScCreate> {
    protected static final boolean EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT);
    // longer recordings on emulator
    protected static final int RECORDING_TIME = EMULATOR ? 6000 : 2000;

    protected Han solo;
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
        solo = new Han(getInstrumentation(), getActivity());
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        if (solo != null) {
            solo.finishOpenedActivities();
        }
        lbm.unregisterReceiver(receiver);
        super.tearDown();
    }

    protected void record(int howlong) {
        record(howlong, solo.getString(R.string.record_instructions));
    }

    protected void record(int howlong, String text) {
        solo.assertText(text);
        assertState(ScCreate.CreateState.IDLE_RECORD);
        solo.clickOnView(R.id.btn_action);
        solo.sleep(howlong);
        assertState(ScCreate.CreateState.RECORD);
        solo.clickOnView(R.id.btn_action);
        solo.assertText(R.string.reset); // "Discard"
        assertState(IDLE_PLAYBACK);
    }

    protected void gotoEditMode() {
        solo.clickOnView(getActivity().findViewById(R.id.btn_edit));
        solo.assertText(R.string.btn_revert_to_original);
        assertState(ScCreate.CreateState.EDIT);
    }

    protected void playback() {
        assertState(ScCreate.CreateState.IDLE_PLAYBACK);
        solo.clickOnView(R.id.btn_play);
        assertState(ScCreate.CreateState.PLAYBACK);
    }

    protected void playbackEdit() {
        assertState(ScCreate.CreateState.EDIT);
        solo.clickOnView(R.id.btn_play_edit);
        assertState(ScCreate.CreateState.EDIT_PLAYBACK);
    }

    protected void assertState(ScCreate.CreateState state) {
        assertTrue(waitForState(state, 5000));
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