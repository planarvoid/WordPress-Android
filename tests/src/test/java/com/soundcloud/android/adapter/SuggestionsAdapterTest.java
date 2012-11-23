package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;

@RunWith(DefaultTestRunner.class)
public class SuggestionsAdapterTest {

    @Test
    public void shouldFindHighlights() throws Exception {
        final Matcher matcher = SuggestionsAdapter.getHighlightPattern("foo").matcher("hallo FoO dsada");
        expect(matcher.find()).toBeTrue();
        expect(matcher.start(2)).toEqual(6);
        expect(matcher.end(2)).toEqual(9);
    }
}
