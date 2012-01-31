package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.util.Calendar;


@SuppressWarnings({"ResultOfMethodCallIgnored"})
@RunWith(DefaultTestRunner.class)
public class RecordingTest {
    static final long USER_ID = 50L;

    Recording r;
    File f;
    Resources res;

    @Before
    public void setup() throws Exception {
        f = new File("/tmp/recording-test");
        r = new Recording(f);
        if (f.exists()) {
            expect(f.delete()).toBeTrue();
        }
        // 14:31:01, 15/02/2011
        Calendar c = Calendar.getInstance();
        c.set(2001, 1, 15, 14, 31, 1);
        r.timestamp = c.getTimeInMillis();
        r.service_ids = "1,2,3";
        r.duration = 86 * 1000;
        res = Robolectric.application.getResources();

        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void itShouldHaveANiceSharingNote() throws Exception {
        expect(r.sharingNote(res)).toEqual("Sounds from Thursday afternoon");
    }

    @Test
    public void shouldGenerateASharingNoteWithLocation() throws Exception {
        r.where_text = "Mars";
        expect(r.sharingNote(res)).toEqual("Sounds from Mars");
    }

    @Test
    public void shouldGenerateASharingNoteWithLocationAndTitle() throws Exception {
        r.what_text = "Party";
        r.where_text = "Mars";
        expect(r.sharingNote(res)).toEqual("Party at Mars");
    }

    @Test
    public void shouldGenerateASharingNoteWithTitle() throws Exception {
        r.what_text = "Party";
        expect(r.sharingNote(res)).toEqual("Party");
    }


    @Test
    public void shouldGenerateStatusWithNotUploaded() throws Exception {
        expect(r.getStatus(res)).toEqual(("10 years, 1.26, not yet uploaded"));
    }

    @Test
     public void shouldGenerateStatusWithError() throws Exception {
        r.upload_error = true;
        expect(r.getStatus(res)).toEqual("10 years, 1.26, upload failed");
     }

    @Test
    public void shouldGenerateStatusWithCurrentlyUploading() throws Exception {
        r.upload_status = 1;
        expect(
                r.getStatus(res)).toEqual(
                "Uploading. You can check on progress in Notifications");
    }

    @Test
    public void shouldHaveFormattedDuration() throws Exception {
        expect(r.formattedDuration()).toEqual("1.26");
    }

    @Test
    public void shouldDeleteRecording() throws Exception {
        expect(r.delete(null)).toBeFalse();
        expect(f.createNewFile()).toBeTrue();
        expect(r.delete(null)).toBeTrue();
        expect(f.exists()).toBeFalse();
    }

    @Test
    public void shouldNotDeleteRecordingIfExternal() throws Exception {
        r.external_upload = true;
        expect(f.createNewFile()).toBeTrue();
        expect(r.delete(null)).toBeFalse();
        expect(f.exists()).toBeTrue();
    }

    @Test
    public void shouldGenerateImageFilename() throws Exception {
        expect(new Recording(new File("/tmp/foo.mp4")).generateImageFile(new File("/images")).getAbsolutePath()).
                toEqual("/images/foo.bmp");

        expect(new Recording(new File("/tmp/foo")).generateImageFile(new File("/images")).getAbsolutePath()).
                toEqual("/images/foo.bmp");
    }

    @Test
    public void shouldGeneratePageTrack() throws Exception {
        Recording r = new Recording(new File("/tmp"));
        expect(r.pageTrack()).toEqual("/record/share/public");
        r.is_private = true;
        expect(r.pageTrack()).toEqual("/record/share/private");
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
        expect(r2.timestamp).toEqual(r.timestamp);
        expect(r2.user_id).toEqual(r.user_id);
        expect(r2.private_user_id).toEqual(r.private_user_id);
        expect(r2.upload_error).toEqual(r.upload_error);
        expect(r2.upload_status).toEqual(r.upload_status);

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
        expect(r3.timestamp).toEqual(r.timestamp);
        expect(r3.user_id).toEqual(r.user_id);
        expect(r3.private_user_id).toEqual(r.private_user_id);
    }

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

    private Recording createRecording() {
        Recording r = new Recording(new File("/tmp"));
        r.latitude = 32.3;
        r.longitude = 23.1;
        r.what_text = "somewhat";
        r.where_text = "somehere";
        r.four_square_venue_id = "foursquare";
        r.duration = 100L;
        r.external_upload = true;
        r.timestamp = 200L;
        r.user_id = USER_ID;
        r.private_user_id = 300L;
        r.upload_error = true;
        r.upload_status = 10;

        return r;
    }
}
