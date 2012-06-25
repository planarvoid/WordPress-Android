package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import scala.remote;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class ContentTest {

    @Test
    public void shouldDefineRequest() throws Exception {
        expect(Content.ME_SOUND_STREAM.request().toUrl())
                .toEqual(Content.ME_SOUND_STREAM.remoteUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNoRemoteUriDefined() throws Exception {
        Content.COLLECTIONS.request();
    }

    @Test
    public void shouldFindContentByUri() throws Exception {
        expect(Content.byUri(Content.ME.uri)).toBe(Content.ME);
    }

    @Test
    public void shouldProvideToString() throws Exception {
        expect(Content.ME_ACTIVITIES.toString()).toEqual("Content.ME_ACTIVITIES");
    }

    @Test
    public void shouldGenerateUriForId() throws Exception {
        expect(Content.COLLECTION_ITEMS.forId(1234).toString()).toEqual(
                "content://com.soundcloud.android.provider.ScContentProvider/collection_items/1234");

        expect(Content.TRACK_ARTWORK.forId(1234).toString()).toEqual(
                "content://com.soundcloud.android.provider.ScContentProvider/tracks/1234/artwork");
    }

    @Test
    public void shouldBuildQuery() throws Exception {
        expect(Content.ME_ACTIVITIES.withQuery("a", "1", "b", "2"))
                .toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/activities/all/own?a=1&b=2");
    }

    @Test
    @Ignore
    public void shouldGetLocalIds() throws Exception {
        final int SIZE = 107;
        final long USER_ID = 1L;
        ContentValues[] cv = new ContentValues[SIZE];
        for (int i = 0; i < SIZE; i++) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.CollectionItems.POSITION, i);
            cv[i].put(DBHelper.CollectionItems.ITEM_ID, i);
            cv[i].put(DBHelper.CollectionItems.USER_ID, USER_ID);
        }

        final ContentResolver contentResolver = Robolectric.application.getContentResolver();
        contentResolver.bulkInsert(Content.ME_FAVORITES.uri, cv);

        expect(Content.ME_FAVORITES.getLocalIds(contentResolver,USER_ID).size()).toEqual(107);

        List<Long> localIds = Content.ME_FAVORITES.getLocalIds(contentResolver, USER_ID, 50, -1);
        expect(localIds.size()).toEqual(57);
        expect(localIds.get(0)).toEqual(50L);

        localIds = Content.ME_FAVORITES.getLocalIds(contentResolver, USER_ID, 100, 50);
        expect(localIds.size()).toEqual(7);
        expect(localIds.get(0)).toEqual(100L);
    }
}
