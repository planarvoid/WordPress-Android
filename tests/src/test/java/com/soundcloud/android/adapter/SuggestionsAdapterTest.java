package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.SearchSuggestions;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowSpannableString;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;

@RunWith(DefaultTestRunner.class)
public class SuggestionsAdapterTest {

    @Test
    public void shouldFindHighlightsSimple() throws Exception {
        Matcher matcher = SuggestionsAdapter.getHighlightPattern("foo").matcher("foo");
        expect(matcher.find()).toBeTrue();
        expect(matcher.start(2)).toEqual(0);
        expect(matcher.end(2)).toEqual(3);

    }
    @Test
    public void shouldFindHighlightsMixedCase() throws Exception {
        Matcher matcher = SuggestionsAdapter.getHighlightPattern("foo").matcher("hallo FoO dsada");
        expect(matcher.find()).toBeTrue();
        expect(matcher.start(2)).toEqual(6);
        expect(matcher.end(2)).toEqual(9);
    }

    @Test
    public void shouldQuerySuggestApi() throws Exception {
        SuggestionsAdapter adapter = new SuggestionsAdapter(DefaultTestRunner.application,
                DefaultTestRunner.application);

        TestHelper.addCannedResponse(SearchSuggestions.class,
                "/search/suggest?q=foo&highlight=true&limit=5", "suggest_highlight.json");

        adapter.runQueryOnBackgroundThread("foo");

        expect(adapter.getRemote().size()).toEqual(3);
        expect(adapter.getLocal().size()).toEqual(0);

        String q1 = adapter.getRemote().suggestions.get(0).query;
        expect(q1).toEqual("<b>Foo</b> Fighters");
    }
}
