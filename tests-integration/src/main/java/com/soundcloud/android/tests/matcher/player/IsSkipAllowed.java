package com.soundcloud.android.tests.matcher.player;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsSkipAllowed extends TypeSafeMatcher<VisualPlayerElement> {

    @Override
    public boolean matchesSafely(VisualPlayerElement playerElement) {
        return isSkipButtonsAvailable(playerElement) || isSkipAreaAvailable(playerElement);
    }

    private boolean isSkipAreaAvailable(VisualPlayerElement playerElement) {
        return playerElement.nextPageArea().isVisible() &&
                playerElement.previousPageArea().isVisible();
    }

    private boolean isSkipButtonsAvailable(VisualPlayerElement playerElement) {
        return playerElement.nextButton().isVisible() &&
                playerElement.previousButton().isVisible();
    }

    public void describeTo(Description description) {
        description.appendText("skip allowed");
    }

    @Factory
    public static Matcher<VisualPlayerElement> SkipAllowed() {
        return new IsSkipAllowed();
    }
}
