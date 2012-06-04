package com.soundcloud.android.activity;

import static com.soundcloud.android.activity.ScCreate.CreateState.IDLE_PLAYBACK;

import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.R;
import com.soundcloud.android.service.upload.UploadService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.test.ActivityInstrumentationTestCase2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class RecordingTestCase extends ActivityInstrumentationTestCase2<ScCreate> {
    protected Solo solo;
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
        intents = Collections.synchronizedList(new ArrayList<Intent>());
        lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(receiver, UploadService.getIntentFilter());
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getActivity().reset();
            }
        });
        solo = new Solo(getInstrumentation(), getActivity());
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
        record(howlong, "Share your Sounds");
    }

    protected void record(int howlong, String text) {
        solo.waitForText(text);
        assertState(ScCreate.CreateState.IDLE_RECORD);
        solo.clickOnView(getActivity().findViewById(R.id.btn_action));
        solo.sleep(howlong);
        assertState(ScCreate.CreateState.RECORD);
        solo.clickOnView(getActivity().findViewById(R.id.btn_action));
        solo.waitForText("Discard");
        assertState(IDLE_PLAYBACK);
    }

    protected void gotoEditMode() {
        solo.clickOnView(getActivity().findViewById(R.id.btn_edit));
        assertTrue(solo.waitForText("Revert to original"));
        assertState(ScCreate.CreateState.EDIT);
    }

    protected void playback() {
        assertState(ScCreate.CreateState.IDLE_PLAYBACK);

        solo.clickOnView(getActivity().findViewById(R.id.btn_play));
        solo.sleep(100);
        assertState(ScCreate.CreateState.PLAYBACK);
    }

    protected void playbackEdit() {
        assertState(ScCreate.CreateState.EDIT);

        solo.clickOnView(getActivity().findViewById(R.id.btn_play_edit));
        solo.sleep(100);
        assertState(ScCreate.CreateState.EDIT_PLAYBACK);
    }


    protected void assertState(ScCreate.CreateState state) {
        assertSame(state, getActivity().getState());
    }


    protected boolean waitForIntent(String action, long timeout) {
        final long startTime = SystemClock.uptimeMillis();
        final long endTime = startTime + timeout;
        while (SystemClock.uptimeMillis() < endTime) {
            solo.sleep(300);
            for (Intent intent : intents) {
                if (action.equals(intent.getAction())) {
                    return true;
                }
            }
        }
        return false;
    }
}