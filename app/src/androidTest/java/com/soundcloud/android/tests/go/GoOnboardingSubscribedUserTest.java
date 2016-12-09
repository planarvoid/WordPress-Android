package com.soundcloud.android.tests.go;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.go.GoOnboardingScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import org.hamcrest.Matcher;

public class GoOnboardingSubscribedUserTest extends ActivityTest<GoOnboardingActivity> {
    private GoOnboardingScreen screen;

    public GoOnboardingSubscribedUserTest() {
        super(GoOnboardingActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.offlineUser;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        networkManagerClient.switchWifiOff();
        screen = new GoOnboardingScreen(solo);
    }

    public void testGoStartLazyLoadingAndRetry() {
        CollectionScreen nextScreen;

        assertThat(screen.startButton(), is(viewVisible()));
        nextScreen = screen.clickStartButton();
        assertThat(nextScreen, is(not(screenVisible())));
        assertThat(screen.retryButton(), is(viewVisible()));

        networkManagerClient.switchWifiOn();
        nextScreen = screen.clickRetryButton();
        assertThat(nextScreen, is(screenVisible()));
    }

    private Matcher<Screen> screenVisible() {
        return com.soundcloud.android.framework.matcher.screen.IsVisible.visible();
    }

    private Matcher<ViewElement> viewVisible() {
        return com.soundcloud.android.framework.matcher.view.IsVisible.visible();
    }

}
