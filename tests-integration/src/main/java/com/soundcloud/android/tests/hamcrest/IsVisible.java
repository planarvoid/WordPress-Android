package com.soundcloud.android.tests.hamcrest;


import com.soundcloud.android.screens.elements.UIView;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsVisible extends TypeSafeMatcher<UIView> {

    @Override
    public boolean matchesSafely(UIView uiView) {
        return uiView.isVisible();
    }

    public void describeTo(Description description) {
        description.appendText("not visible");
    }

    @Factory
    public static Matcher<UIView> Visible() {
        return new IsVisible();
    }
}
