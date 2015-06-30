package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.SearchSuggestions;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.testsupport.fixtures.JsonFixtures;
import com.soundcloud.android.api.legacy.Request;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.app.SearchManager;
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

    private SuggestionsAdapter adapter;

    @Mock private PublicApi mockApi;
    @Mock private ContentResolver contentResolver;
    @Mock private Cursor cursor;
    @Mock private Context context;

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
        mockPublicApi("suggest_highlight.json");
        createAdapter();

        adapter.runQueryOnBackgroundThread("foo");
        shadowOf(adapter.getApiTaskLooper()).runToEndOfTasks();

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
        mockPublicApi("suggest_mixed.json");
        createAdapter();

        adapter.runQueryOnBackgroundThread("foo");
        shadowOf(adapter.getApiTaskLooper()).runToEndOfTasks();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startService(intentCaptor.capture());

        Intent syncIntent = intentCaptor.getValue();

        List<Uri> uris = syncIntent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS);
        expect(uris.size()).toEqual(3);
        expect(uris.get(0)).toEqual(Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/tracks/q/196380%2C196381"));
        expect(uris.get(1).toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/users/q/2097360");
        expect(uris.get(2).toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/playlists/q/324731");
    }

    @Test
    public void shouldGetTrackUrnForTrackItemPosition() {
        mockCursorTrackId();
        createAdapter();

        Urn urn = adapter.getUrn(0);

        expect(urn.isTrack()).toBeTrue();
        expect(urn.getNumericId()).toEqual(1L);
    }

    @Test
    public void shouldGetUserUrnForUserItemPosition() {
        mockCursorUserId();
        createAdapter();

        Urn urn = adapter.getUrn(0);

        expect(urn.isUser()).toBeTrue();
        expect(urn.getNumericId()).toEqual(1L);
    }

    @Test
    public void shouldGetQueryUrnForItemPosition() {
        mockCursorQueryUrn();
        createAdapter();

        Urn urn = adapter.getQueryUrn(0);

        expect(urn.toString()).toEqual("soundcloud:search:123");
    }

    @Test
    public void shouldGetQueryPositionForItemPosition() {
        mockCursorQueryPosition();
        createAdapter();

        int position = adapter.getQueryPosition(0);

        expect(position).toEqual(5);
    }

    @Test
    public void shouldReportCorrectSourceForPosition() throws IOException {
        mockPublicApi("suggest_mixed.json");
        mockCursorSource(1);
        createAdapter();

        adapter.runQueryOnBackgroundThread("foo");

        expect(adapter.isLocalResult(0)).toBeTrue();
    }

    private void mockCursorContentType(Uri contentUri) {
        when(cursor.moveToNext()).thenReturn(true, false);
        final int fakeContentUriColumn = 1;
        when(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA)).thenReturn(fakeContentUriColumn);
        when(cursor.getString(fakeContentUriColumn)).thenReturn(contentUri.toString());
    }

    private void mockCursorSource(int isLocal) {
        mockCursorContentType(Content.TRACK.forId(1L));
        final int fakeSourceColumn = 2;
        when(cursor.getColumnIndex(SuggestionsAdapter.LOCAL)).thenReturn(fakeSourceColumn);
        when(cursor.getInt(fakeSourceColumn)).thenReturn(isLocal);
    }

    private void mockCursorTrackId() {
        mockCursorContentType(Content.TRACK.forId(1L));
        final int fakeIdColumn = 3;
        when(cursor.getColumnIndex(TableColumns.Suggestions.ID)).thenReturn(fakeIdColumn);
        when(cursor.getLong(fakeIdColumn)).thenReturn(1L);
    }

    private void mockCursorUserId() {
        mockCursorContentType(Content.USER.forId(1L));
        final int fakeIdColumn = 3;
        when(cursor.getColumnIndex(TableColumns.Suggestions.ID)).thenReturn(fakeIdColumn);
        when(cursor.getLong(fakeIdColumn)).thenReturn(1L);
    }

    private void mockCursorQueryUrn() {
        mockCursorContentType(Content.USER.forId(1L));
        final int fakeIdColumn = 7;
        when(cursor.getColumnIndex(SuggestionsAdapter.QUERY_URN)).thenReturn(fakeIdColumn);
        when(cursor.getString(fakeIdColumn)).thenReturn("soundcloud:search:123");
    }

    private void mockCursorQueryPosition() {
        mockCursorContentType(Content.USER.forId(1L));
        final int fakeIdColumn = 8;
        when(cursor.getColumnIndex(SuggestionsAdapter.QUERY_POSITION)).thenReturn(fakeIdColumn);
        when(cursor.getInt(fakeIdColumn)).thenReturn(5);
    }

    private void mockContext() {
        when(context.getApplicationContext()).thenReturn(mock(SoundCloudApplication.class));
        when(context.getResources()).thenReturn(mock(Resources.class));

        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        ConnectivityManager cm = mock(ConnectivityManager.class);
        when(cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)).thenReturn(networkInfo);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(cm);

        when(contentResolver.query(any(Uri.class), any(String[].class), anyString(), any(String[].class), anyString()))
                .thenReturn(cursor);
    }

    private void mockPublicApi(String fixture) throws IOException {
        when(mockApi.getMapper()).thenReturn(TestHelper.getObjectMapper());

        final String suggestionsJson = JsonFixtures.resourceAsString(SearchSuggestions.class, fixture);
        HttpResponse response = new TestHttpResponse(200, suggestionsJson);
        when(mockApi.get(any(Request.class))).thenReturn(response);
    }

    private void createAdapter() {
        mockContext();
        adapter = new SuggestionsAdapter(context, mockApi, contentResolver);
        adapter.changeCursor(cursor);
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
