package com.soundcloud.android.framework.matcher.player;

import com.soundcloud.android.framework.screens.elements.VisualPlayerElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsSkipAllowed extends TypeSafeMatcher<VisualPlayerElement> {

    @Override
    public boolean matchesSafely(VisualPlayerElement playerElement) {
        return isSkipButtonsAvailable(playerElement) || isSkipAdAvailable(playerElement);
    }

    private boolean isSkipAdAvailable(VisualPlayerElement playerElement) {
        return playerElement.isSkippable();
    }

    private boolean isSkipButtonsAvailable(VisualPlayerElement playerElement) {
        return playerElement.isNextButtonClickable() && playerElement.isPreviousButtonClickable();
    }

    public void describeTo(Description description) {
        description.appendText("skip allowed.");
    }

    @Factory
    public static Matcher<VisualPlayerElement> SkipAllowed() {
        return new IsSkipAllowed();
    }
}
