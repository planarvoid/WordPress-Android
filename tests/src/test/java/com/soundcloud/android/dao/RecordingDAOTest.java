package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.Content;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.soundcloud.android.Expect.expect;

public class RecordingDAOTest extends BaseDAOTest {

    @Test
    public void shouldUpdateARecording() throws Exception {
        Recording r = createRecording();
        ContentResolver resolver = Robolectric.application.getContentResolver();
        Uri u = resolver.insert(Content.RECORDINGS.uri, r.buildContentValues());

        expect(u).not.toBeNull();

        final Cursor c = resolver.query(u, null, null, null, null);
        expect(c.moveToNext()).toBeTrue();
        Recording r2 = new Recording(c);
        r2.where_text = "changed";
        expect(resolver.update(u, r2.buildContentValues(), null, null)).toEqual(1);

        final Cursor c2 = resolver.query(u, null, null, null, null);
        expect(c2.moveToNext()).toBeTrue();
        Recording r3 = new Recording(c2);
        expect(r3.where_text).toEqual("changed");
    }

    @Test
    public void shouldGetRecordingFromIntentViaDatabase() throws Exception {
        final ContentResolver contentResolver = Robolectric.application.getContentResolver();
        Recording r = Recording.fromUri(RecordingDAO.insert(createRecording(), contentResolver), contentResolver);

        assert r != null;
        Intent i = new Intent().setData(r.toUri());

        Recording r2 = Recording.fromIntent(i, contentResolver, -1);
        expect(r2).not.toBeNull();
        expect(r2.description).toEqual(r.description);
        expect(r2.is_private).toEqual(r.is_private);
        expect(r2.where_text).toEqual(r.where_text);
        expect(r2.what_text).toEqual(r.what_text);
    }

    @Test
    public void shouldPersistAndLoadCorrectly() throws Exception {
        Recording r = createRecording();
        ContentResolver resolver = Robolectric.application.getContentResolver();

        Uri uri = resolver.insert(Content.RECORDINGS.uri, r.buildContentValues());
        expect(uri).not.toBeNull();

        // all recordings, with username joined in
        Cursor cursor = resolver.query(Content.RECORDINGS.uri, null, null, null, null);
        expect(cursor).not.toBeNull();
        expect(cursor.getCount()).toEqual(1);
        expect(cursor.moveToFirst()).toBeTrue();

        Recording r2 = new Recording(cursor);

        expect(r2.id).not.toEqual(r.id);
        expect(r2.id).toEqual(1L);
        expect(r2.latitude).toEqual(r.latitude);
        expect(r2.longitude).toEqual(r.longitude);
        expect(r2.what_text).toEqual(r.what_text);
        expect(r2.where_text).toEqual(r.where_text);
        expect(r2.duration).toEqual(r.duration);
        expect(r2.external_upload).toEqual(r.external_upload);
        expect(r2.user_id).toEqual(r.user_id);
        expect(r2.recipient_user_id).toEqual(r.recipient_user_id);
        expect(r2.upload_status).toEqual(r.upload_status);
        expect(r2.tip_key).toEqual(r.tip_key);

        // just this recording
        cursor = resolver.query(uri, null, null, null, null);

        expect(cursor).not.toBeNull();
        expect(cursor.getCount()).toEqual(1);
        expect(cursor.moveToFirst()).toBeTrue();

        Recording r3 = new Recording(cursor);
        expect(r3.id).not.toEqual(r.id);
        expect(r3.latitude).toEqual(r.latitude);
        expect(r3.longitude).toEqual(r.longitude);
        expect(r3.what_text).toEqual(r.what_text);
        expect(r3.where_text).toEqual(r.where_text);
        expect(r3.duration).toEqual(r.duration);
        expect(r3.external_upload).toEqual(r.external_upload);
        expect(r3.user_id).toEqual(r.user_id);
        expect(r3.recipient_user_id).toEqual(r.recipient_user_id);
    }

    private Recording createRecording() throws IOException {
        File tmp = new File("/tmp/recording");

        Recording r = new Recording(tmp);
        r.latitude = 32.3;
        r.longitude = 23.1;
        r.what_text = "somewhat";
        r.where_text = "somehere";
        r.four_square_venue_id = "foursquare";
        r.description = "test recording";
        r.genre = "speed blues ";
        r.duration = 86 * 1000;
        r.user_id = USER_ID;
        r.  recipient_user_id = 300L;
        r.recipient_username = "foo";
        r.shared_emails = "foo@example.com";
        r.shared_ids = "1,2,3,4";
        r.upload_status = Recording.Status.NOT_YET_UPLOADED;
        r.artwork_path = r.getFile();
        r.resized_artwork_path = r.artwork_path;
        r.tip_key = "something";
        return r;
    }
}
