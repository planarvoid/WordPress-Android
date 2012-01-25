package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
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
    public void shouldInsertQueryAndDeleteFavorites() throws Exception {
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);
        Uri uri;
        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_FAVORITES.uri, t.buildContentValues())).not.toBeNull();
        }

        Cursor c = resolver.query(Content.ME_FAVORITES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(15);

        resolver.delete(Content.ME_FAVORITES.uri, DBHelper.CollectionItems.ITEM_ID + " = ?",
                new String[]{String.valueOf(tracks.get(0).id)});

        c = resolver.query(Content.ME_FAVORITES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(14);
    }

    @Test
    public void shouldCleanup() throws Exception {
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_FAVORITES.uri, t.buildContentValues())).not.toBeNull();
        }

        expect(resolver.query(Content.TRACKS.uri, null, null, null, null).getCount()).toEqual(15);
        expect(resolver.query(Content.USERS.uri, null, null, null, null).getCount()).toEqual(14);

        tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("tracks.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.TRACKS.uri, t.buildContentValues())).not.toBeNull();
        }

        expect(resolver.query(Content.TRACKS.uri, null, null, null, null).getCount()).toEqual(18);
        expect(resolver.query(Content.USERS.uri, null, null, null, null).getCount()).toEqual(15);

        resolver.update(Content.TRACK_CLEANUP.uri, null, null, null);
        expect(resolver.query(Content.TRACKS.uri, null, null, null, null).getCount()).toEqual(15);

        resolver.update(Content.USERS_CLEANUP.uri, null, null, null);
        expect(resolver.query(Content.USERS.uri, null, null, null, null).getCount()).toEqual(14);
    }


    @Test
    public void shouldIncludeUsernameForPrivateRecordings() throws Exception {
        Recording r = new Recording(new File("/tmp/test"));
        r.user_id = USER_ID;
        r.private_user_id = USER_ID;

        User user = new User();
        user.id = USER_ID;
        user.username = "current user";

        expect(resolver.insert(Content.USERS.uri, user.buildContentValues())).not.toBeNull();
        Uri uri = resolver.insert(Content.RECORDINGS.uri, r.buildContentValues());

        expect(uri).not.toBeNull();

        Cursor c = resolver.query(Content.RECORDINGS.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToFirst()).toBeTrue();

        expect(c.getString(c.getColumnIndex("username"))).toEqual("current user");
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

        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA)))
                .toEqual("soundcloud:tracks:22365800");

        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1)))
                .toEqual(Content.TRACK_ARTWORK.forId(22365800L).toString());

        expect(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)))
                .toEqual(22365800L);
    }

    @Test
    public void shouldGetCurrentUserId() throws Exception {
        Cursor c = resolver.query(Content.ME_USERID.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToFirst()).toBeTrue();
        expect(c.getLong(0)).toEqual(USER_ID);
    }
}
