package com.soundcloud.android.tests.matcher.view;


import com.soundcloud.android.screens.elements.UIView;
import com.soundcloud.android.tests.ViewElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsEnabled extends TypeSafeMatcher<ViewElement> {

    @Override
    public boolean matchesSafely(ViewElement uiView) {
        return uiView.isEnabled();
    }

    public void describeTo(Description description) {
        description.appendText("enabled");
    }

    @Factory
    public static Matcher<ViewElement> Enabled() {
        return new IsEnabled();
    }
}
