package com.soundcloud.android.stream;

import static com.soundcloud.android.tests.TestUser.streamUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTestCase;

public class StreamTest extends ActivityTestCase<LauncherActivity> {

    private StreamScreen streamScreen;

    public StreamTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        streamUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testStreamShouldHaveCorrectTitle() {
        streamScreen = new StreamScreen(solo);
        assertThat(streamScreen.getTitle(), is(equalToIgnoringCase("Stream")));
    }

    public void testStreamContainsItems() {
        streamScreen = new StreamScreen(solo);
        assertThat(streamScreen.getItemCount(), is(greaterThan(0)));
    }

}
