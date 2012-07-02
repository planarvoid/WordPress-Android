package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.Actions;
import com.soundcloud.android.audio.reader.WavReader;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;


@SuppressWarnings({"ResultOfMethodCallIgnored"})
@RunWith(DefaultTestRunner.class)
public class RecordingTest {
    static final long USER_ID = 50L;
    Resources res;

    @Before
    public void setup() throws Exception {
        res = Robolectric.application.getResources();
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void itShouldHaveANiceSharingNote() throws Exception {
        Recording r = createRecording();
        r.what_text = null;
        r.where_text = null;
        expect(r.sharingNote(res)).toEqual("Sounds from Thursday afternoon");
    }

    @Test
    public void shouldGenerateASharingNoteWithLocation() throws Exception {
        Recording r = createRecording();
        r.what_text = null;
        r.where_text = "Mars";
        expect(r.sharingNote(res)).toEqual("Sounds from Mars");
    }

    @Test
    public void shouldGenerateASharingNoteWithLocationAndTitle() throws Exception {
        Recording r = createRecording();
        r.what_text = "Party";
        r.where_text = "Mars";
        expect(r.sharingNote(res)).toEqual("Party at Mars");
    }

    @Test
    public void shouldGenerateASharingNoteWithTitle() throws Exception {
        Recording r = createRecording();
        r.what_text = "Party";
        r.where_text = null;
        expect(r.sharingNote(res)).toEqual("Party");
    }

    @Test
    public void shouldGenerateStatusWithNotUploaded() throws Exception {
        Recording r = createRecording();
        expect(r.getStatus(res)).toMatch("1? years, 1\\.26, not yet uploaded");
    }

    @Test
    public void shouldGenerateStatusWithError() throws Exception {
        Recording r = createRecording();
        r.upload_status = Recording.Status.ERROR;
        expect(r.getStatus(res)).toMatch("1? years, 1\\.26, upload failed");
    }

    @Test
    public void shouldGenerateStatusWithCurrentlyUploading() throws Exception {
        Recording r = createRecording();
        r.upload_status = Recording.Status.UPLOADING;
        expect(
                r.getStatus(res)).toEqual(
                "Uploading. You can check on progress in Notifications");
    }

    @Test
    public void shouldHaveFormattedDuration() throws Exception {
        Recording r = createRecording();
        expect(r.formattedDuration()).toEqual("1.26");
    }

    @Test
    public void shouldDeleteRecording() throws Exception {
        Recording r = createRecording();
        expect(r.exists()).toBeTrue();
        expect(r.delete(null)).toBeTrue();
        expect(r.exists()).toBeFalse();
    }

    @Test
    public void shouldNotDeleteRecordingIfExternal() throws Exception {
        Recording r = createRecording();
        r.external_upload = true;
        expect(r.delete(null)).toBeFalse();
        expect(r.exists()).toBeTrue();
    }

    @Test
    public void shouldDeterminedLastModifiedFromFile() throws Exception {
        Recording r = createRecording();
        expect(r.lastModified()).toEqual(r.getFile().lastModified());
    }

    @Test
    public void shouldGenerateImageFilename() throws Exception {
        expect(new Recording(new File("/tmp/foo.wav")).generateImageFile(new File("/images")).getAbsolutePath()).
                toEqual("/images/foo.bmp");

        expect(new Recording(new File("/tmp/foo")).generateImageFile(new File("/images")).getAbsolutePath()).
                toEqual("/images/foo.bmp");
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

    private Recording createRecording() throws IOException {
        File tmp = File.createTempFile("recording-test", "wav");
        tmp.createNewFile();
        expect(tmp.exists()).toBeTrue();

        Calendar c = Calendar.getInstance();
        //noinspection MagicConstant
        c.set(2001, 1, 15, 14, 31, 1);  // 14:31:01, 15/02/2011
        tmp.setLastModified(c.getTimeInMillis());

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
        r.recipient_user_id = 300L;
        r.recipient_username = "foo";
        r.shared_emails = "foo@example.com";
        r.shared_ids = "1,2,3,4";
        r.upload_status = Recording.Status.NOT_YET_UPLOADED;
        r.artwork_path = r.getFile();
        r.resized_artwork_path = r.artwork_path;

        return r;
    }

    @Test
    public void shouldGetRecordingFromIntent() throws Exception {
        Intent i = new Intent(Actions.SHARE)
                .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File("/tmp")))
                .putExtra(Actions.EXTRA_DESCRIPTION, "description")
                .putExtra(Actions.EXTRA_GENRE, "genre")
                .putExtra(Actions.EXTRA_PUBLIC, false)
                .putExtra(Actions.EXTRA_TITLE, "title")
                .putExtra(Actions.EXTRA_WHERE, "where")
                .putExtra(Actions.EXTRA_TAGS, new String[] { "tags" })
                ;

        Recording r = Recording.fromIntent(i, Robolectric.application.getContentResolver(), -1);
        expect(r).not.toBeNull();
        expect(r.description).toEqual("description");
        expect(r.genre).toEqual("genre");
        expect(r.is_private).toBeTrue();
        expect(r.where_text).toEqual("where");
        expect(r.what_text).toEqual("title");
        expect(r.tagString()).toEqual("tags soundcloud:source=android-3rdparty-upload");
    }

    @Test
    public void shouldGetRecordingFromIntentViaDatabase() throws Exception {
        Recording r = SoundCloudDB.insertRecording(Robolectric.application.getContentResolver(), createRecording());

        assert r != null;
        Intent i = new Intent().setData(r.toUri());

        Recording r2 = Recording.fromIntent(i, Robolectric.application.getContentResolver(), -1);
        expect(r2).not.toBeNull();
        expect(r2.description).toEqual(r.description);
        expect(r2.is_private).toEqual(r.is_private);
        expect(r2.where_text).toEqual(r.where_text);
        expect(r2.what_text).toEqual(r.what_text);
    }

    @Test
    public void shouldGetRecordingFromIntentViaParcelable() throws Exception {
        Recording r = createRecording();
        Intent i = new Intent().putExtra(SoundRecorder.EXTRA_RECORDING, r);
        Recording r2 = Recording.fromIntent(i, Robolectric.application.getContentResolver(), -1);
        expect(r2).not.toBeNull();
        expect(r2.description).toEqual(r.description);
        expect(r2.is_private).toEqual(r.is_private);
        expect(r2.where_text).toEqual(r.where_text);
        expect(r2.what_text).toEqual(r.what_text);
    }

    @Test
    public void shouldGenerateTagString() throws Exception {
        Recording r = createRecording();
        r.tags = new String[]{"foo baz", "bar", "baz"};
        expect(r.tagString()).toContain("\"foo baz\" bar baz");
    }

    @Test
    public void shouldAddDedicatedTagIfPrivateMessage() throws Exception {
        Recording r = createRecording();
        r.recipient_user_id = 10;
        expect(r.getTags()).toContain("soundcloud:recording-type=dedicated");
    }

    @Test
    public void shouldAddFoursquareMachineTags() throws Exception {
        Recording r = createRecording();
        r.four_square_venue_id = "abcdef";
        expect(r.getTags()).toContain("foursquare:venue=abcdef");
    }

    @Test
    public void shouldSetADifferentMachineTagWhenDoing3rdPartyUpload() throws Exception {
        Recording r = createRecording();
        r.external_upload = true;
        List<String> tags = r.getTags();
        expect(tags).toContain("soundcloud:source=android-3rdparty-upload");
        expect(tags).not.toContain("soundcloud:source=android-record");
    }

    @Test
    public void shouldSetGeoMachineTags() throws Exception {
        Recording r = createRecording();
        r.longitude = 0.1d;
        r.latitude = 0.2d;
        List<String> tags = r.getTags();
        expect(tags).toContain("geo:lon=0.1");
        expect(tags).toContain("geo:lat=0.2");
    }

    @Test
    public void shouldBeParcelable() throws Exception {
        Recording r = createRecording();

        Parcel p = Parcel.obtain();
        r.writeToParcel(p, 0);

        Recording r2 = Recording.CREATOR.createFromParcel(p);

        expect(r.id).toEqual(r2.id);
        expect(r.user_id).toEqual(r2.user_id);
        expect(r.what_text).toEqual(r2.what_text);
        expect(r.where_text).toEqual(r2.where_text);
        expect(r.duration).toEqual(r2.duration);
        expect(r.description).toEqual(r2.description);
        expect(r.genre).toEqual(r2.genre);
        expect(r.longitude).toEqual(r2.longitude);
        expect(r.latitude).toEqual(r2.latitude);
        expect(r.getFile()).toEqual(r2.getFile());
        expect(r.getEncodedFile()).toEqual(r2.getEncodedFile());
        expect(r.artwork_path).toEqual(r2.artwork_path);
        expect(r.resized_artwork_path).toEqual(r2.resized_artwork_path);
        expect(r.four_square_venue_id).toEqual(r2.four_square_venue_id);
        expect(r.shared_emails).toEqual(r2.shared_emails);
        expect(r.shared_ids).toEqual(r2.shared_ids);
        expect(r.recipient_username).toEqual(r2.recipient_username);
        expect(r.recipient_user_id).toEqual(r2.recipient_user_id);
        expect(r.external_upload).toEqual(r2.external_upload);
    }


    @Test
    public void shouldGetUploadFile() throws Exception {
        Recording r = createRecording();
        expect(r.getUploadFile()).not.toBeNull();
        expect(r.getUploadFile()).toBe(r.getFile());
    }

    @Test
    public void shouldGetArtwork() throws Exception {
        Recording r = createRecording();
        expect(r.hasArtwork()).toBeTrue();
        expect(r.getArtwork()).toBe(r.artwork_path);
        r.resized_artwork_path = File.createTempFile("resized-artwork", "png");
        expect(r.getArtwork()).toBe(r.resized_artwork_path);

        r.artwork_path = r.resized_artwork_path = null;
        expect(r.hasArtwork()).toBeFalse();
    }

    @Test
    public void shouldGetUserIdFromFile() throws Exception {
        expect(Recording.getUserIdFromFile(new File("_/foo"))).toEqual(-1l);
        expect(Recording.getUserIdFromFile(new File("_/foo/12232_1234"))).toEqual(1234l);
        expect(Recording.getUserIdFromFile(new File("/foo/12232_1234.ogg"))).toEqual(1234l);
        expect(Recording.getUserIdFromFile(new File("foo/12232_1234.ogg_"))).toEqual(1234l);
        expect(Recording.getUserIdFromFile(new File("_foo/12232_1234.ogg_"))).toEqual(1234l);
    }

    @Test
    public void shouldHavetoUri() throws Exception {
        Recording r = createRecording();
        expect(r.toUri()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/recordings");
        r.id = 10;
        expect(r.toUri()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/recordings/10");
    }

    @Test
    public void testIsEncodedFilename() throws Exception {
        expect(Recording.isEncodedFilename("foo.ogg")).toBeTrue();
        expect(Recording.isEncodedFilename("foo.wav")).toBeFalse();
    }

    @Test
    public void testIsRawFilename() throws Exception {
        expect(Recording.isRawFilename("foo.ogg")).toBeFalse();
        expect(Recording.isRawFilename("foo.wav")).toBeTrue();
    }

    @Test
    public void shouldCreateRecordingWithWavFileExtensionByDefault() throws Exception {
        Recording r = Recording.create();
        expect(IOUtils.extension(r.getFile())).toEqual(WavReader.EXTENSION);
    }
}
