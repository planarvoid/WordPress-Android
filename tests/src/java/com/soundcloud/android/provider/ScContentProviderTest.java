package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.io.File;

@RunWith(DefaultTestRunner.class)
public class ScContentProviderTest {
    static final long USER_ID = 100L;

    ContentResolver resolver;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
    }

    @Test
    public void shouldInsertAndQueryRecordings() throws Exception {
        Recording r = new Recording(new File("/tmp/test"));
        r.user_id = USER_ID;

        Uri uri = resolver.insert(Content.RECORDINGS.uri, r.buildContentValues());
        expect(uri).not.toBeNull();

        Cursor c = resolver.query(Content.RECORDINGS.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
    }

    @Test
    public void shouldSupportAndroidGlobalSearch() throws Exception {
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            resolver.insert(Content.TRACKS.uri, t.buildContentValues());
            resolver.insert(Content.USERS.uri, t.user.buildContentValues());
        }

        Cursor cursor = resolver.query(Content.ANDROID_SEARCH_SUGGEST.uri,
                null, null, new String[] { "plaid"}, null);

        expect(cursor.getCount()).toEqual(1);
        expect(cursor.moveToFirst()).toBeTrue();
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)))
                .toEqual("Plaid - missing (taken from new album Scintilli)");

        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_2)))
                .toEqual("Warp Records");

        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID)))
                .toEqual(SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT);

        expect(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)))
                .toEqual(22365800L);
    }
}
