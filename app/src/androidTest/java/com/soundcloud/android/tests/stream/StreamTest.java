package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

public class StreamTest extends ActivityTest<LauncherActivity> {

    private StreamScreen streamScreen;

    public StreamTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        streamUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testStreamShouldHaveCorrectTitle() {
        streamScreen = new StreamScreen(solo);
        assertThat(streamScreen.getTitle(), is(equalToIgnoringCase("Stream")));
    }

    public void testStreamContainsItems() {
        streamScreen = new StreamScreen(solo);
        assertThat(streamScreen.getItemCount(), is(greaterThan(0)));
    }

    public void testStreamLoadsNextPage() {
        streamScreen = new StreamScreen(solo);
        int itemsBeforePaging = streamScreen.getItemCount();
        streamScreen.scrollToBottomOfPage();
        assertThat(streamScreen.getItemCount(), is(greaterThan(itemsBeforePaging)));
    }
}
