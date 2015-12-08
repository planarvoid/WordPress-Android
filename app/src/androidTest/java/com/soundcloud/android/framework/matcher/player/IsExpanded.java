package com.soundcloud.android.framework.matcher.player;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsExpanded extends TypeSafeMatcher<VisualPlayerElement> {

    @Override
    public boolean matchesSafely(VisualPlayerElement playerElement) {
        return playerElement.waitForExpandedPlayer().isExpanded();
    }

    public void describeTo(Description description) {
        description.appendText("expanded");
    }

    @Factory
    public static Matcher<VisualPlayerElement> expanded() {
        return new IsExpanded();
    }
}
