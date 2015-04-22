package com.soundcloud.android.framework.matcher.player;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsPlaying extends TypeSafeMatcher<VisualPlayerElement> {

    @Override
    public boolean matchesSafely(VisualPlayerElement playerElement) {
        return playerElement.waitForPlayState();
    }

    public void describeTo(Description description) {
        description.appendText("playing");
    }

    @Factory
    public static Matcher<VisualPlayerElement> playing() {
        return new IsPlaying();
    }
}
