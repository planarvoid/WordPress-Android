package com.soundcloud.android.tests.matcher.player;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsCollapsed extends TypeSafeMatcher<VisualPlayerElement> {

    @Override
    public boolean matchesSafely(VisualPlayerElement playerElement) {
        return playerElement.isCollapsed();
    }

    public void describeTo(Description description) {
        description.appendText("collapsed");
    }

    @Factory
    public static Matcher<VisualPlayerElement> collapsed() {
        return new IsCollapsed();
    }
}
