package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.model.SearchSuggestions.Query;

import com.soundcloud.android.adapter.SuggestionsAdapter;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.SearchManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.BaseColumns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RunWith(DefaultTestRunner.class)
public class SearchSuggestionsTest {
    @Test
    public void shouldDeserializeCorrectly() throws Exception {
        SearchSuggestions suggestions = TestHelper.readJson(SearchSuggestions.class,
                "/com/soundcloud/android/model/suggest_users.json");

        expect(suggestions.tx_id).toEqual("92dbb484c0d144afa6c193ece99514f3");
        expect(suggestions.query_time_in_millis).toEqual(1l);
        expect(suggestions.query).toEqual("f");
        expect(suggestions.limit).toEqual(5);
        expect(suggestions.suggestions).toNumber(5);

        expect(suggestions.suggestions.get(0).query).toEqual("Foo Fighters");
        expect(suggestions.suggestions.get(0).kind).toEqual("user");
        expect(suggestions.suggestions.get(0).id).toEqual(2097360l);
        expect(suggestions.suggestions.get(0).score).toEqual(889523l);
        expect(suggestions.suggestions.get(0).getClientUri().toString()).toEqual("soundcloud:users:2097360");
        expect(suggestions.suggestions.get(0).getIntentData()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/users/2097360");
    }

    @Test
    public void shouldReturnResolverIconUriIfNotSet() throws Exception {
        Query q = new Query();
        q.kind = "user";
        q.id = 123;
        expect(q.getIconUri()).toEqual(
                "https://api.soundcloud.com/resolve/image?url=soundcloud%3Ausers%3A123&client_id=40ccfee680a844780a41fbe23ea89934");
    }

    @Test
    public void shouldInializeFromLocalResults() throws Exception {
        MatrixCursor cursor = new MatrixCursor(SuggestionsAdapter.COLUMN_NAMES);
        cursor.addRow(new Object[] {
                1,
                123,
                "foo user",
                Content.USER.forId(123).toString(),
                "http://i1.sndcdn.com/avatars-000002315321-2z3mh1-large.jpg",
        });
        cursor.addRow(new Object[] {
                2,
                1234,
                "foo track",
                Content.TRACK.forId(1234).toString(),
                "http://i1.soundcloud.com/artworks-000004875808-1qu95c-large.jpg",
        });
        SearchSuggestions suggestions = new SearchSuggestions(cursor);
        expect(suggestions.size()).toEqual(2);

        Iterator<SearchSuggestions.Query> it = suggestions.iterator();
        Query q1 = it.next();
        Query q2 = it.next();

        expect(q1.kind).toEqual("user");
        expect(q1.id).toEqual(123l);
        expect(q1.query).toEqual("foo user");
        expect(q1.getIconUri()).toEqual("http://i1.sndcdn.com/avatars-000002315321-2z3mh1-large.jpg");
        expect(q1.getIntentData()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/users/123");

        expect(q2.kind).toEqual("track");
        expect(q2.id).toEqual(1234l);
        expect(q2.query).toEqual("foo track");
        expect(q2.getIconUri()).toEqual("http://i1.soundcloud.com/artworks-000004875808-1qu95c-large.jpg");
        expect(q2.getIntentData()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/tracks/1234");
    }

    @Test
    public void shouldMergeTwoSuggestionsIntoOne() throws Exception {
        MatrixCursor cursor = new MatrixCursor(SuggestionsAdapter.COLUMN_NAMES);
        cursor.addRow(new Object[] {
                1,
                123,
                "foo user",
                Content.USER.forId(123).toString(),
                "http://i1.sndcdn.com/avatars-000002315321-2z3mh1-large.jpg",
        });
        cursor.addRow(new Object[] {
                2,
                1234,
                "foo track",
                Content.TRACK.forId(1234).toString(),
                "http://i1.soundcloud.com/artworks-000004875808-1qu95c-large.jpg",
        });
        // duplicate!
        cursor.addRow(new Object[] {
                3,
                2097360,
                "Foo Fighters",
                Content.USER.forId(2097360).toString(),
                "http://i1.soundcloud.com/artworks-000004875808-1qu95c-large.jpg"
        });
        SearchSuggestions local = new SearchSuggestions(cursor);
        SearchSuggestions remote = TestHelper.readJson(SearchSuggestions.class,
                "/com/soundcloud/android/model/suggest_users.json");

        SearchSuggestions merged = local.merge(remote);

        expect(merged.size()).toEqual(7); // 5 remote + 3 local (1 dup)
        Iterator<Query> it = merged.iterator();
        expect(it.next().isLocal()).toBeTrue();
        expect(it.next().isLocal()).toBeTrue();
        expect(it.next().isLocal()).toBeTrue();
        expect(it.next().isLocal()).toBeFalse();
        expect(it.next().isLocal()).toBeFalse();
        expect(it.next().isLocal()).toBeFalse();
        expect(it.next().isLocal()).toBeFalse();
        expect(it.hasNext()).toBeFalse();
    }

    @Test
    public void shouldConvertHighlightsToCursorFormat() {
        SearchSuggestions suggestions = new SearchSuggestions();
        Query suggestion = new Query();
        ArrayList<Map<String, Integer>> highlights = new ArrayList<Map<String, Integer>>(1);
        highlights.add(new HashMap<String, Integer>());
        highlights.add(new HashMap<String, Integer>());
        highlights.get(0).put("pre", 0);
        highlights.get(0).put("post", 3);
        highlights.get(1).put("pre", 6);
        highlights.get(1).put("post", 9);

        suggestion.kind = "user";
        suggestion.highlights = highlights;
        suggestions.add(suggestion);

        Cursor c = suggestions.asCursor();
        expect(c.moveToNext()).toBeTrue();
        expect(c.getString(c.getColumnIndex(SuggestionsAdapter.HIGHLIGHTS))).toEqual("0,3;6,9");
    }

    @Test
    public void shouldConvertSuggestionsBackToCursor() throws Exception {
        MatrixCursor cursor = new MatrixCursor(SuggestionsAdapter.COLUMN_NAMES);
        cursor.addRow(new Object[] {
                1,
                123,
                "foo user",
                Content.USER.forId(123).toString(),
                "http://i1.sndcdn.com/avatars-000002315321-2z3mh1-large.jpg",
        });
        SearchSuggestions suggestions = new SearchSuggestions(cursor);
        Cursor c = suggestions.asCursor();
        expect(c.moveToNext()).toBeTrue();
        expect(c.getLong(c.getColumnIndex(BaseColumns._ID))).toEqual(-1l);
        expect(c.getLong(c.getColumnIndex(DBHelper.Suggestions.ID))).toEqual(123l);
        expect(c.getString(c.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))).toEqual("foo user");
        expect(c.getString(c.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA))).toEqual(Content.USER.forId(123).toString());
        expect(c.getString(c.getColumnIndex(DBHelper.Suggestions.ICON_URL))).toEqual("http://i1.sndcdn.com/avatars-000002315321-2z3mh1-large.jpg");
        expect(c.moveToNext()).toBeFalse();
    }

    @Test
    public void shouldIgnoreUnsupportedTypes() throws IOException {
        SearchSuggestions suggestions = TestHelper.readJson(SearchSuggestions.class,
                "/com/soundcloud/android/model/suggest_mixed.json");

        expect(suggestions.asCursor().getCount()).toBe(3); // ignore playlists for now
        expect(suggestions.suggestions.get(0).kind).toEqual(Query.KIND_USER);
        expect(suggestions.suggestions.get(1).kind).toEqual(Query.KIND_TRACK);
        expect(suggestions.suggestions.get(2).kind).toEqual(Query.KIND_TRACK);
    }

    @Test
    public void testResolveKindFromContentUri() {
        expect(Query.kindFromContentUri(Content.TRACK.uri)).toEqual(Query.KIND_TRACK);
        expect(Query.kindFromContentUri(Content.TRACKS.uri)).toEqual(Query.KIND_TRACK);
        expect(Query.kindFromContentUri(Content.USER.uri)).toEqual(Query.KIND_USER);
        expect(Query.kindFromContentUri(Content.USERS.uri)).toEqual(Query.KIND_USER);
        expect(Query.kindFromContentUri(Content.PLAYLIST.uri)).toEqual(Query.KIND_PLAYLIST);
        expect(Query.kindFromContentUri(Content.PLAYLISTS.uri)).toEqual(Query.KIND_PLAYLIST);
    }

    @Test
    public void shouldResolveToClientUri() throws IOException {
        SearchSuggestions suggestions = TestHelper.readJson(SearchSuggestions.class,
                "/com/soundcloud/android/model/suggest_mixed.json");

        expect(suggestions.suggestions.get(0).getClientUri()).toEqual(new ClientUri("soundcloud:users:2097360"));
        expect(suggestions.suggestions.get(1).getClientUri()).toEqual(new ClientUri("soundcloud:tracks:196380"));
        expect(suggestions.suggestions.get(2).getClientUri()).toEqual(new ClientUri("soundcloud:tracks:196381"));
        expect(suggestions.suggestions.get(3).getClientUri()).toEqual(new ClientUri("soundcloud:playlists:324731"));
    }

    @Test
    public void shouldAddRemoteResourceIdsForPrefetching() throws IOException {
        ArrayList<Long> trackIds = new ArrayList<Long>();
        ArrayList<Long> playlistIds = new ArrayList<Long>();
        ArrayList<Long> userIds = new ArrayList<Long>();

        SearchSuggestions suggestions = TestHelper.readJson(SearchSuggestions.class,
                "/com/soundcloud/android/model/suggest_mixed.json");

        suggestions.putRemoteIds(trackIds, userIds, playlistIds);

        expect(trackIds.contains(196380L)).toBeTrue();
        expect(userIds.contains(2097360L)).toBeTrue();
        expect(playlistIds.contains(324731L)).toBeTrue();
    }
}
