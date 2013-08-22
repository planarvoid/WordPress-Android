package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

import android.content.ContentResolver;
import android.database.Cursor;

public class RecordingDAOTest extends AbstractDAOTest<RecordingDAO> {

    public RecordingDAOTest() {
        super(new RecordingDAO(Robolectric.application.getContentResolver()));
    }

    @Test
    public void shouldUpdateARecording() throws Exception {
        Recording r = TestHelper.createRecording(OWNER_ID);
        TestHelper.insertWithDependencies(r);

        r.where_text = "changed";
        getDAO().update(r);

        final Cursor c = resolver.query(r.toUri(), null, null, null, null);
        expect(c.moveToNext()).toBeTrue();
        expect(new Recording(c).where_text).toEqual("changed");
    }

    @Test
    public void shouldPersistAndLoadCorrectly() throws Exception {
        Recording r = TestHelper.createRecording(OWNER_ID);
        ContentResolver resolver = Robolectric.application.getContentResolver();

        getDAO().create(r);

        // all recordings, with username joined in
        Cursor cursor = resolver.query(Content.RECORDINGS.uri, null, null, null, null);
        expect(cursor).not.toBeNull();
        expect(cursor.getCount()).toEqual(1);
        expect(cursor.moveToFirst()).toBeTrue();

        Recording r2 = new Recording(cursor);

        expect(r2.getId()).toEqual(r.getId());
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
        cursor = resolver.query(r.toUri(), null, null, null, null);

        expect(cursor).not.toBeNull();
        expect(cursor.getCount()).toEqual(1);
        expect(cursor.moveToFirst()).toBeTrue();

        Recording r3 = new Recording(cursor);
        expect(r3.getId()).toEqual(r.getId());
        expect(r3.latitude).toEqual(r.latitude);
        expect(r3.longitude).toEqual(r.longitude);
        expect(r3.what_text).toEqual(r.what_text);
        expect(r3.where_text).toEqual(r.where_text);
        expect(r3.duration).toEqual(r.duration);
        expect(r3.external_upload).toEqual(r.external_upload);
        expect(r3.user_id).toEqual(r.user_id);
        expect(r3.recipient_user_id).toEqual(r.recipient_user_id);
    }

}
