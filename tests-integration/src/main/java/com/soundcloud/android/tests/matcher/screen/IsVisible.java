package com.soundcloud.android.tests.matcher.screen;

import com.soundcloud.android.screens.Screen;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsVisible extends TypeSafeMatcher<Screen> {

    @Override
    public boolean matchesSafely(Screen screen) {
        return screen.isVisible();
    }

    public void describeTo(Description description) {
        description.appendText("visible");
    }

    @Factory
    public static Matcher<Screen> visible() {
        return new IsVisible();
    }
}
