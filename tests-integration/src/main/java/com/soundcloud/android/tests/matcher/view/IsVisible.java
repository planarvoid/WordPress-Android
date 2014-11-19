package com.soundcloud.android.tests.matcher.view;


import com.soundcloud.android.tests.viewelements.ViewElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsVisible extends TypeSafeMatcher<ViewElement> {

    @Override
    public boolean matchesSafely(ViewElement viewElement) {
        return viewElement.isVisible();
    }

    public void describeTo(Description description) {
        description.appendText("visible");
    }

    @Factory
    public static Matcher<ViewElement> visible() {
        return new IsVisible();
    }
}
