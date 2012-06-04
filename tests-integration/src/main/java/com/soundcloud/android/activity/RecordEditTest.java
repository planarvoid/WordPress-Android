package com.soundcloud.android.activity;

import com.jayway.android.robotium.solo.Solo;
import com.soundcloud.android.R;

import android.content.Intent;
import android.net.Uri;

public class RecordEditTest extends RecordingTestCase {

    public void testEditModesGetPersisted() {
        record(RECORDING_TIME);
        gotoEditMode();

        playbackEdit();

        solo.clickOnText("No Fading");
        solo.clickOnText("Not Optimized");
        solo.clickOnText("Save");

        assertState(ScCreate.CreateState.IDLE_PLAYBACK);

        assertTrue(solo.isToggleButtonChecked("Fades on"));
        assertTrue(solo.isToggleButtonChecked("Optimized"));

        Uri recUri = getActivity().getRecorder().getRecording().toUri();
        assertNotNull(recUri);

        // start new activity just with uri
        ScCreate create = launchActivityWithIntent("com.soundcloud.android", ScCreate.class, new Intent().setData(recUri));
        assertSame(ScCreate.CreateState.IDLE_PLAYBACK, create.getState());
        Solo solo2 = new Solo(getInstrumentation(), create);
        solo2.clickOnView(create.findViewById(R.id.btn_edit));
        solo2.sleep(100);
        assertSame(ScCreate.CreateState.EDIT, create.getState());

        assertTrue(solo2.isToggleButtonChecked("Fades on"));
        assertTrue(solo2.isToggleButtonChecked("Optimized"));
    }
}
