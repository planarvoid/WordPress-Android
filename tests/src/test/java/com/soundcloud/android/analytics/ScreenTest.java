package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ScreenTest {

    @Test
    public void shouldGetTrackingTag() {
        expect(Screen.EXPLORE_GENRES.get()).toEqual("explore:genres");
    }

    @Test
    public void shouldGetTrackingTagWithAppendedPath() {
        expect(Screen.EXPLORE_GENRES.get("path")).toEqual("explore:genres:path");
    }

    @Test
    public void gettingTagWithAppendedPathShouldNormalizePath() {
        expect(Screen.EXPLORE_GENRES.get("Hello & World")).toEqual("explore:genres:hello_&_world");
    }
}
