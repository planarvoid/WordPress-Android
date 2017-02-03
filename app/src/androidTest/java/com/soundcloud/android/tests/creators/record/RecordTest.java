package com.soundcloud.android.tests.creators.record;

import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.matcher.screen.IsVisible;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.record.RecordMetadataScreen;
import com.soundcloud.android.screens.record.RecordScreen;
import com.soundcloud.android.tests.ActivityTest;

public class RecordTest extends ActivityTest<MainActivity> {
    private RecordScreen recordScreen;

    public RecordTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.recordUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testRecordFlow() {
        recordScreen = mainNavHelper.goToRecord();
        recordScreen.deleteRecordingIfPresent(); // start clean

        create();
        checkIsSaved();
        delete();

        createAfterPausing();
        edit();
        play();
        upload();
    }

    private void create() {
        assertThat(recordScreen.getTitle(), is(solo.getString(R.string.rec_title_idle_rec)));
        recordScreen.startRecording();
        assertThat(recordScreen.getTitle(), is(solo.getString(R.string.rec_title_recording)));
        assertThat(recordScreen.hasNextButton(), is(false));

        recordScreen.stopRecording();
        assertThat(recordScreen.getTitle(), is(solo.getString(R.string.rec_title_idle_play)));
        assertThat(recordScreen.hasNextButton(), is(true));
        assertThat(recordScreen.hasRecordedTrack(), is(true));
    }


    private void checkIsSaved() {
        solo.goBack();

        recordScreen = mainNavHelper.goToRecord();
        assertThat(recordScreen.hasRecordedTrack(), is(true));
    }

    private void delete() {
        recordScreen.deleteRecording();
        assertThat(recordScreen.hasRecordedTrack(), is(false));
    }

    private void createAfterPausing() {
        recordScreen
                .startRecording()
                .waitAndPauseRecording();

        String firstChrono = recordScreen.getChronometer();

        recordScreen
                .startRecording()
                .waitAndPauseRecording();

        assertThat(recordScreen.getChronometer(), is(not(firstChrono)));
        assertThat(recordScreen.getEditButton(), is(visible()));
    }

    private void edit() {
        recordScreen.clickEditButton();
        assertThat(recordScreen.getEditButton(), is(not(visible())));
        assertThat(recordScreen.getApplyButton(), is(visible()));
        assertThat(recordScreen.getRevertButton(), is(visible()));

        recordScreen.clickApplyButton();
        assertThat(recordScreen.getApplyButton(), is(not(visible())));
        assertThat(recordScreen.getNextButton(), is(visible()));
    }

    private void play() {
        recordScreen.clickPlayButton();

        assertThat(recordScreen.getTitle(), is(solo.getString(R.string.rec_title_playing)));
    }

    private void upload() {
        RecordMetadataScreen metadataScreen = recordScreen
                .clickNext();

        String title = "test-" + System.currentTimeMillis();

        recordScreen = metadataScreen
                .setPrivate()
                .setTitle(title)
                .clickUploadButton();

        assertThat(recordScreen, is(IsVisible.visible()));
        assertThat(recordScreen.hasNextButton(), is(false));

        solo.goBack();

        ProfileScreen profileScreen = mainNavHelper.goToMyProfile();
        assertThat(profileScreen, is(IsVisible.visible()));

        if (!profileScreen.getFirstTrackTitle().equals(title)) {
            profileScreen.pullToRefresh();
        }
        assertThat(profileScreen.getFirstTrackTitle(), is(title));
    }
}
