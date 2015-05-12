package com.soundcloud.android.tests.creators.record;

import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringStartsWith.startsWith;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.record.RecordScreen;
import com.soundcloud.android.tests.ActivityTest;

public class RecordTest extends ActivityTest<MainActivity> {
    private RecordScreen recordScreen;

    public RecordTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.recordUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        recordScreen = new StreamScreen(solo).actionBar().clickRecordButton();
        recordScreen.deleteRecordingIfPresent(); // start clean
    }

    @Override
    protected void tearDown() throws Exception {
        recordScreen.deleteRecordingIfPresent(); // cleanup
        super.tearDown();
    }

    public void testRecordButton() {
        recordScreen.startRecording();
        assertThat(recordScreen.getChronometer(), is("0:00"));
        assertThat(recordScreen.getNextButton(), is(not(visible())));

        recordScreen.waitAndPauseRecording();
        assertThat(recordScreen.getChronometer(), is(not("0:00")));
        assertThat(recordScreen.getNextButton(), is(visible()));
    }

    public void testRecordingIsDeletable() {
        recordScreen
                .startRecording()
                .waitAndPauseRecording();
        assertThat(recordScreen.getDeleteButton(), is(visible()));

        recordScreen.deleteRecording();
        assertThat(recordScreen.getDeleteButton(), is(not(visible())));
    }

    public void testRecordingIsSaved() {
        assertThat(recordScreen.getDeleteButton(), is(not(visible())));

        recordScreen
                .startRecording()
                .waitAndPauseRecording();

        solo.goBack();

        recordScreen = new StreamScreen(solo).actionBar().clickRecordButton();
        assertThat(recordScreen.getDeleteButton(), is(visible()));
    }

    public void testRecordingIsEditable() {
        recordScreen
                .startRecording()
                .waitAndPauseRecording();

        assertThat(recordScreen.getEditButton(), is(visible()));

        recordScreen.clickEditButton();
        assertThat(recordScreen.getEditButton(), is(not(visible())));
        assertThat(recordScreen.getApplyButton(), is(visible()));
        assertThat(recordScreen.getRevertButton(), is(visible()));

        recordScreen.clickApplyButton();
        assertThat(recordScreen.getApplyButton(), is(not(visible())));
        assertThat(recordScreen.getNextButton(), is(visible()));
    }

    public void testRecordingIsResumable() {
        recordScreen
                .startRecording()
                .waitAndPauseRecording();

        String firstChrono = recordScreen.getChronometer();

        recordScreen
                .startRecording()
                .waitAndPauseRecording();

        assertThat(recordScreen.getChronometer(), is(not(firstChrono)));
    }

    public void testRecordingIsPlayable() {
        recordScreen
                .startRecording()
                .waitAndPauseRecording()
                .clickPlayButton();

        // Note: it might record more than two seconds.
        assertThat(recordScreen.getChronometer(), startsWith("0:00 / 0:"));
    }
}
