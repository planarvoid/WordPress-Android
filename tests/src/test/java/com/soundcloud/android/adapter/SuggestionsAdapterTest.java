package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.model.SearchSuggestions;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.net.Uri;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

@RunWith(DefaultTestRunner.class)
public class SuggestionsAdapterTest {

    @Before
    public void setup() {
        TestHelper.connectedViaWifi(true);
    }

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

    @Test
    public void shouldPrefetchResources() throws IOException {
        TestApplication context = DefaultTestRunner.application;
        SuggestionsAdapter adapter = new SuggestionsAdapter(context, context);

        TestHelper.addCannedResponse(SearchSuggestions.class,
                "/search/suggest?q=foo&highlight=true&limit=5", "suggest_mixed.json");

        adapter.runQueryOnBackgroundThread("foo");

        ShadowApplication shadowApplication = Robolectric.shadowOf(context);
        Intent syncIntent = shadowApplication.getNextStartedService();

        expect(syncIntent).not.toBeNull();
        List<Uri> uris = syncIntent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS);
        expect(uris.size()).toEqual(3);
        expect(uris.get(0)).toEqual(Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/tracks/196380%2C196381"));
        expect(uris.get(1).toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/users/2097360");
        expect(uris.get(2).toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/playlists/324731");
    }

    // TODO: shadow for support-v4 CursorAdapter doesn't work. RL 2.0 may fix this.
//    @DisableStrictI18n
//    @Test
//    public void shouldRenderResultRows() {
//        SuggestionsAdapter adapter = new SuggestionsAdapter(DefaultTestRunner.application,
//                DefaultTestRunner.application);
//
//        SearchSuggestions suggestions = new SearchSuggestions();
//        suggestions.add(new SearchSuggestions.Query() {
//            {
//                id = 1;
//                query = "foo";
//                kind = "user";
//                score = 1;
//            }
//        });
//
//        Cursor cursor = suggestions.asCursor();
//        adapter.changeCursor(cursor);
//
//        for (int i=0; i<cursor.getCount(); i++) {
//            cursor.moveToPosition(i);
//            adapter.newView(DefaultTestRunner.application, cursor, null);
//        }
//
//    }
}
