package com.soundcloud.android.tests.stream;


import static com.soundcloud.android.framework.TestUser.emptyUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

public class EmptyStreamTest extends ActivityTest<MainActivity> {

    protected StreamScreen streamScreen;

    public EmptyStreamTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        emptyUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    public void testShowsEmptyStreamScreen() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertTrue(streamScreen.emptyView().isVisible());
    }

    public void testClickOnPeopleToFollowOpensSearch() {
        assertThat(streamScreen.clickOnFindPeopleToFollow(), is(visible()));
    }
}