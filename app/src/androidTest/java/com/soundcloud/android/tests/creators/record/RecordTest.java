package com.soundcloud.android.tests.creators.record;

import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
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
        recordScreen = mainNavHelper.goToRecord();
        recordScreen.deleteRecordingIfPresent(); // start clean
    }

    public void testRecordButton() {
        assertThat(recordScreen.getTitle(), is(solo.getString(R.string.rec_title_idle_rec)));
        recordScreen.startRecording();
        assertThat(recordScreen.getTitle(), is(solo.getString(R.string.rec_title_recording)));
        assertThat(recordScreen.hasNextButton(), is(false));

        recordScreen.waitAndPauseRecording();
        assertThat(recordScreen.getTitle(), is(solo.getString(R.string.rec_title_idle_play)));
        assertThat(recordScreen.hasNextButton(), is(true));
    }

    public void testRecordingIsDeletable() {
        recordScreen
                .startRecording()
                .waitAndPauseRecording();
        assertThat(recordScreen.hasRecordedTrack(), is(true));

        recordScreen.deleteRecording();
        assertThat(recordScreen.hasRecordedTrack(), is(false));
    }

    public void testRecordingIsSaved() {
        recordScreen
                .startRecording()
                .waitAndPauseRecording();

        solo.goBack();

        recordScreen = mainNavHelper.goToRecord();
        assertThat(recordScreen.hasRecordedTrack(), is(true));
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

        assertThat(recordScreen.getTitle(), is(solo.getString(R.string.rec_title_playing)));
    }
}
