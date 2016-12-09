package com.soundcloud.android.tests.go;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.Element;
import com.soundcloud.android.screens.elements.GoOnboardingErrorElement;
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
    protected TestUser getUserForLogin() {
        return TestUser.freeNonMonetizedUser;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        screen = new GoOnboardingScreen(solo);
    }

    public void testSubscriptionFailed() {
        final GoOnboardingErrorElement errorElement = screen.clickStartButtonForError();
        assertThat(errorElement, is(elementVisible()));

        Screen nextScreen = errorElement.clickTryLater();
        assertThat(nextScreen, is(screenVisible()));
    }

    private Matcher<Element> elementVisible() {
        return com.soundcloud.android.framework.matcher.element.IsVisible.visible();
    }

    private Matcher<Screen> screenVisible() {
        return com.soundcloud.android.framework.matcher.screen.IsVisible.visible();
    }

}
