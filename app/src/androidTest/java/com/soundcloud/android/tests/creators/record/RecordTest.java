package com.soundcloud.android.tests.creators.record;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.record.RecordScreen;
import com.soundcloud.android.screens.StreamScreen;
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
    }

    public void testRecordScreenIsVisible() {
        assertThat(recordScreen, is(visible()));
    }

    // big brother test :D
    public void testRecordButtonStartsRecording() {
        recordScreen
                .deleteRecordingIfPresent()
                .clickRecordButton(); // start recording

        waiter.waitTwoSeconds();

        recordScreen
                .clickRecordButton()
                .clickNext();
        assertNotSame(recordScreen.getChronometer().getText(), "0:00");

        // cleanup
        recordScreen.deleteRecordingIfPresent();
    }
}
