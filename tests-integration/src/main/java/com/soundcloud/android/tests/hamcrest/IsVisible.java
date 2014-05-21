package com.soundcloud.android.tests.hamcrest;


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
        description.appendText("not visible");
    }

    @Factory
    public static Matcher<Screen> Visible() {
        return new IsVisible();
    }
}
