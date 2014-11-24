package com.soundcloud.android.framework.matcher.view;


import com.soundcloud.android.framework.viewelements.ViewElement;
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
