package com.soundcloud.android.tests.creators.upload;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
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
        recordScreen = new StreamScreen(solo).actionBar().clickRecordButton();
    }

    public void testMetadataScreenIsVisible() {
        recordScreen
                .deleteRecordingIfPresent()
                .clickRecordButton(); // start recording

        waiter.waitTwoSeconds();

        recordMetadataScreen = recordScreen
                .clickRecordButton() // pause
                .clickNext();

        assertThat(recordMetadataScreen, is(visible()));
    }
}
