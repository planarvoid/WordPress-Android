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

public class GoOnboardingNotSubscribedTest extends ActivityTest<GoOnboardingActivity> {
    private GoOnboardingScreen screen;

    public GoOnboardingNotSubscribedTest() {
        super(GoOnboardingActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.freeNonMonetizedUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        screen = new GoOnboardingScreen(solo);
    }

    public void testSubscriptionFailed() {
        CollectionScreen nextScreen;

        nextScreen = screen.clickStartButton();
        assertThat(nextScreen, is(not(screenVisible())));
        assertThat(screen.errorTitle(), is(viewVisible()));

        nextScreen = screen.clickTryLater();
        assertThat(nextScreen, is(screenVisible()));
    }

    private Matcher<Screen> screenVisible() {
        return com.soundcloud.android.framework.matcher.screen.IsVisible.visible();
    }

    private Matcher<ViewElement> viewVisible() {
        return com.soundcloud.android.framework.matcher.view.IsVisible.visible();
    }

}
