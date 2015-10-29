package com.soundcloud.android.tests.creators.upload;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.record.RecordMetadataScreen;
import com.soundcloud.android.screens.record.RecordScreen;
import com.soundcloud.android.tests.ActivityTest;

public class MetadataTest extends ActivityTest<MainActivity> {
    private RecordScreen recordScreen;
    private RecordMetadataScreen recordMetadataScreen;

    public MetadataTest() {
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
        recordScreen.deleteRecordingIfPresent();
    }

    public void testMetadataUploadPrivate() {
        RecordMetadataScreen metadataScreen = recordScreen
                .startRecording()
                .waitAndPauseRecording()
                .clickNext();

        String title = "test-" + System.currentTimeMillis();

        recordScreen = metadataScreen
                .setPrivate()
                .setTitle(title)
                .clickUploadButton();

        assertThat(recordScreen, is(visible()));
        assertThat(recordScreen.hasNextButton(), is(false));

        solo.goBack();

        ProfileScreen profileScreen = mainNavHelper.goToMyProfile();
        assertThat(profileScreen, is(visible()));
        assertThat(profileScreen.getFirstTrackTitle(), is(title));
    }
}
