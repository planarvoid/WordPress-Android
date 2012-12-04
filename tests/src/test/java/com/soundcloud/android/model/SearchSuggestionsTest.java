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
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Iterator;

@RunWith(DefaultTestRunner.class)
public class SearchSuggestionsTest {
    @Test
    public void shouldDeserializeCorrectly() throws Exception {
        SearchSuggestions suggestions = TestHelper.readJson(SearchSuggestions.class,
                "/com/soundcloud/android/model/suggest.json");

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
                "/com/soundcloud/android/model/suggest.json");

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
}
