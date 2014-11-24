package com.soundcloud.android.framework.matcher.element;

import com.soundcloud.android.framework.screens.elements.Element;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsVisible extends TypeSafeMatcher<Element> {

    @Override
    public boolean matchesSafely(Element element) {
        return element.isVisible();
    }

    public void describeTo(Description description) {
        description.appendText("visible");
    }

    @Factory
    public static Matcher<Element> visible() {
        return new IsVisible();
    }
}
