package com.soundcloud.android.tests.creators.record;

import static com.soundcloud.android.framework.TestUser.recordUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.RecordScreen;
import com.soundcloud.android.tests.ActivityTest;

public class RecordTest extends ActivityTest<LauncherActivity> {
    private RecordScreen recordScreen;

    public RecordTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        recordUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        recordScreen = menuScreen.clickSystemSettings().actionBar().clickRecordButton();
    }

    public void testRecordScreenIsVisible() {
        assertThat(recordScreen, is(visible()));
    }

    // big brother test :D
    public void testRecordButtonStartsRecording() {
        deleteOldRecordingIfPresent();

        recordScreen.clickRecordButton(); // start recording
        assertThat(recordScreen.getChronometer().getText(), is("0:00"));

        waiter.waitTwoSeconds();

        recordScreen.clickRecordButton(); // pause
        assertNotSame(recordScreen.getChronometer().getText(), "0:00");
    }

    private void deleteOldRecordingIfPresent() {
        if(recordScreen.hasOldRecording()) {
            recordScreen.getDeleteOldRecordingButton().click();
            solo.waitForDialogToOpen(2000l);
            recordScreen.acceptDeleteRecording();
            solo.waitForDialogToClose(2000l);
        }
    }
}
