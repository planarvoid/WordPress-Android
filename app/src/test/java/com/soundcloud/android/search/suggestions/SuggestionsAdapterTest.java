package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.model.SearchSuggestions;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

@RunWith(SoundCloudTestRunner.class)
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
        PublicApi mockApi = mockPublicApi("suggest_highlight.json");

        SuggestionsAdapter adapter = new SuggestionsAdapter(mockContext(), mockApi);
        adapter.runQueryOnBackgroundThread("foo");

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockApi).get(requestCaptor.capture());
        expect(requestCaptor.getValue().toUrl()).toEqual("/search/suggest?q=foo&highlight_mode=offsets&limit=5");

        expect(adapter.getRemote().size()).toEqual(3);
        expect(adapter.getLocal().size()).toEqual(0);

        SearchSuggestions.Query suggestion = adapter.getRemote().suggestions.get(0);
        expect(suggestion.query).toEqual("Foo Fighters");
        expect(suggestion.highlights.size()).toEqual(1);
        expect(suggestion.highlights.get(0).get("pre")).toEqual(0);
        expect(suggestion.highlights.get(0).get("post")).toEqual(3);
    }

    @Test
    public void shouldPrefetchResources() throws IOException {
        TestHelper.connectedViaWifi(true);
        PublicApi mockApi = mockPublicApi("suggest_mixed.json");

        final Context context = mockContext();
        SuggestionsAdapter adapter = new SuggestionsAdapter(context, mockApi);
        adapter.runQueryOnBackgroundThread("foo");

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startService(intentCaptor.capture());

        Intent syncIntent = intentCaptor.getValue();

        List<Uri> uris = syncIntent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS);
        expect(uris.size()).toEqual(3);
        expect(uris.get(0)).toEqual(Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/tracks/q/196380%2C196381"));
        expect(uris.get(1).toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/users/q/2097360");
        expect(uris.get(2).toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/playlists/q/324731");
    }

    private Context mockContext() {
        Context context = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        when(context.getApplicationContext()).thenReturn(mock(SoundCloudApplication.class));
        when(context.getContentResolver()).thenReturn(mockResolver);
        when(context.getResources()).thenReturn(mock(Resources.class));

        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        ConnectivityManager cm = mock(ConnectivityManager.class);
        when(cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).thenReturn(networkInfo);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(cm);

        when(mockResolver.query(any(Uri.class), any(String[].class), anyString(), any(String[].class), anyString())).thenReturn(mock(Cursor.class));
        return context;
    }

    private PublicApi mockPublicApi(String fixture) throws IOException {
        PublicApi mockApi = mock(PublicApi.class);
        when(mockApi.getMapper()).thenReturn(TestHelper.getObjectMapper());

        final String suggestionsJson = TestHelper.resourceAsString(SearchSuggestions.class, fixture);
        HttpResponse response = new TestHttpResponse(200, suggestionsJson);
        when(mockApi.get(any(Request.class))).thenReturn(response);
        return mockApi;
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
