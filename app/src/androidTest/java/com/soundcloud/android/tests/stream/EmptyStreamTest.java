package com.soundcloud.android.tests.stream;


import static com.soundcloud.android.framework.TestUser.emptyUser;

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

    public void testShowsEmptyStreamScreen() {
        streamScreen = new StreamScreen(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertTrue(streamScreen.emptyView().isVisible());
    }
}
