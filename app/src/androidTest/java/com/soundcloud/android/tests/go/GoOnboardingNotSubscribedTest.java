package com.soundcloud.android.tests.go;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.freeNonMonetizedUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.forcePendingPlanUpgrade;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.matcher.screen.IsVisible;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.Element;
import com.soundcloud.android.screens.elements.GoOnboardingErrorElement;
import com.soundcloud.android.screens.go.GoOnboardingScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import org.hamcrest.Matcher;
import org.junit.Test;

public class GoOnboardingNotSubscribedTest extends ActivityTest<GoOnboardingActivity> {
    private GoOnboardingScreen screen;

    public GoOnboardingNotSubscribedTest() {
        super(GoOnboardingActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return freeNonMonetizedUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        screen = new GoOnboardingScreen(solo);
    }

    @Override
    protected void beforeActivityLaunched() {
        forcePendingPlanUpgrade(getInstrumentation().getTargetContext());
    }

    @Test
    public void testSubscriptionFailed() throws Exception {
        final GoOnboardingErrorElement errorElement = screen.clickStartButtonForError();
        assertThat(errorElement, is(elementVisible()));

        Screen nextScreen = errorElement.clickTryLater();
        assertThat(nextScreen, is(screenVisible()));
    }

    private Matcher<Element> elementVisible() {
        return visible();
    }

    private Matcher<Screen> screenVisible() {
        return IsVisible.visible();
    }

}
