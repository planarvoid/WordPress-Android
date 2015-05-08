package com.soundcloud.android.api.legacy.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.Actions;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.creators.record.reader.VorbisReader;
import com.soundcloud.android.creators.record.reader.WavReader;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Params;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Parcel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


@RunWith(SoundCloudTestRunner.class)
public class RecordingTest {
    static final long USER_ID = 50L;
    Resources resources;

    @Before
    public void setup() throws Exception {
        resources = Robolectric.application.getResources();
    }

    @Test
    public void itShouldHaveANiceSharingNote() throws Exception {
        Recording r = createRecording();
        r.title = null;
        expect(r.sharingNote(resources)).toEqual("Sounds from Thursday afternoon");
    }

    @Test
    public void shouldGenerateASharingNoteWithTitle() throws Exception {
        Recording r = createRecording();
        r.setTitle("Party");
        expect(r.sharingNote(resources)).toEqual("Party");
    }

    @Test
    public void shouldGenerateStatusMessageWithNotUploaded() throws Exception {
        Recording r = createRecording();
        expect(r.getStatusMessage(resources)).toMatch("Pending Upload");
    }

    @Test
    public void shouldGenerateStatusMessageWithError() throws Exception {
        Recording r = createRecording();
        r.upload_status = Recording.Status.ERROR;
        expect(r.getStatusMessage(resources)).toMatch("Upload failed");
    }

    @Test
    public void shouldGenerateStatusMessageWithCurrentlyUploading() throws Exception {
        Recording r = createRecording();
        r.upload_status = Recording.Status.UPLOADING;
        expect(r.getStatusMessage(resources)).toEqual("Uploading...");
    }

    @Test
    public void shouldHaveFormattedDuration() throws Exception {
        Recording r = createRecording();
        expect(r.formattedDuration()).toEqual("1:26");
    }

    @Test
    public void existsShouldCheckForRawAndEncodedFile() throws Exception {
        Recording r = createRecording();
        expect(r.getEncodedFile().createNewFile()).toBeTrue();
        expect(r.getFile().delete()).toBeTrue();
        expect(r.exists()).toBeTrue();
        expect(r.getEncodedFile().delete()).toBeTrue();
        expect(r.exists()).toBeFalse();
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


    private Recording createRecording() throws IOException {
        File tmp = createRecordingFile("wav");

        Recording r = new Recording(tmp);
        r.title = "somewhat";
        r.description = "test recording";
        r.genre = "speed blues ";
        r.duration = 86 * 1000;
        r.user_id = USER_ID;
        r.upload_status = Recording.Status.NOT_YET_UPLOADED;
        r.artwork_path = r.getFile();
        r.resized_artwork_path = r.artwork_path;

        return r;
    }

    private File createRecordingFile(String extension) throws IOException {
        File tmp = File.createTempFile("recording-test", extension);
        tmp.createNewFile();
        expect(tmp.exists()).toBeTrue();

        Calendar c = Calendar.getInstance();
        //noinspection MagicConstant
        c.set(2001, 1, 15, 14, 31, 1);  // 14:31:01, 15/02/2011
        tmp.setLastModified(c.getTimeInMillis());
        return tmp;
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
                .putExtra(Actions.EXTRA_TAGS, new String[]{"tags"})
                ;

        Recording r = Recording.fromIntent(i, Robolectric.application, -1);
        expect(r).not.toBeNull();
        expect(r.description).toEqual("description");
        expect(r.genre).toEqual("genre");
        expect(r.is_private).toBeTrue();
        expect(r.title).toEqual("title");
        expect(r.tagString()).toEqual("tags soundcloud:source=android-3rdparty-upload");
    }

    @Test
    public void shouldGetRecordingFromIntentViaParcelable() throws Exception {
        Recording r = createRecording();
        Intent i = new Intent().putExtra(SoundRecorder.EXTRA_RECORDING, r);
        Recording r2 = Recording.fromIntent(i, Robolectric.application, -1);
        expect(r2).not.toBeNull();
        expect(r2.description).toEqual(r.description);
        expect(r2.is_private).toEqual(r.is_private);
        expect(r2.title).toEqual(r.title);
    }

    @Test
    public void shouldReturnNullFromGetRecordingFromIntentForNullIntent() throws Exception {
        expect(Recording.fromIntent(null, null, 0)).toBeNull();
    }

    @Test
    public void shouldGenerateTagString() throws Exception {
        Recording r = createRecording();
        r.tags = new String[]{"foo baz", "bar", "baz"};
        expect(r.tagString()).toContain("\"foo baz\" bar baz");
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
    public void shouldBeParcelable() throws Exception {
        Recording r = createRecording();

        Parcel p = Parcel.obtain();
        r.writeToParcel(p, 0);

        Recording r2 = Recording.CREATOR.createFromParcel(p);

        expect(r.getId()).toEqual(r2.getId());
        expect(r.user_id).toEqual(r2.user_id);
        expect(r.title).toEqual(r2.title);
        expect(r.duration).toEqual(r2.duration);
        expect(r.description).toEqual(r2.description);
        expect(r.genre).toEqual(r2.genre);
        expect(r.getFile()).toEqual(r2.getFile());
        expect(r.getEncodedFile()).toEqual(r2.getEncodedFile());
        expect(r.artwork_path).toEqual(r2.artwork_path);
        expect(r.resized_artwork_path).toEqual(r2.resized_artwork_path);
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

    @Test
    public void shouldHaveEncodedFilenameBasedOnFilename() throws Exception {
        Recording r = Recording.create();
        expect(IOUtils.extension(r.getEncodedFile())).toEqual(VorbisReader.EXTENSION);
        expect(r.getEncodedFile().getName()).not.toContain(WavReader.EXTENSION);
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    public static String FAKE_DATA = "welcome_to_the_junitungle";
    public static final Long FAKE_DATA_LENGTH = Long.valueOf(FAKE_DATA.length());


    @Test
    public void shouldTrimOneFile() throws Exception {
        fillTempDirectory(1);
        final File root = tempFolder.getRoot();
        long size = IOUtils.getDirSize(root);
        expect(Recording.trimWaveFiles(root,null,size - 1)).toEqual(FAKE_DATA_LENGTH);
    }

    @Test
    public void shouldNotTrimIgnoredFile() throws Exception {
        Recording r = fillTempDirectory(1);
        final File root = tempFolder.getRoot();
        long size = IOUtils.getDirSize(root);
        expect(Recording.trimWaveFiles(root, r, size - 1)).toEqual(0l);
    }

    @Test
    public void shouldTrimOneFile2() throws Exception {
        Recording r = fillTempDirectory(2);
        final File root = tempFolder.getRoot();
        expect(Recording.trimWaveFiles(root, r, 1)).toEqual(FAKE_DATA_LENGTH);
    }

    @Test
    public void shouldTrimOneFile3() throws Exception {
        Recording r = fillTempDirectory(3);
        final File root = tempFolder.getRoot();
        long size = IOUtils.getDirSize(root);
        expect(Recording.trimWaveFiles(root, r, size - 1)).toEqual(FAKE_DATA_LENGTH);
    }

    @Test
    public void shouldTrimTwoFiles() throws Exception {
        Recording r = fillTempDirectory(3);
        final File root = tempFolder.getRoot();
        expect(Recording.trimWaveFiles(root, r, 1)).toEqual(2* FAKE_DATA_LENGTH);
    }

    private Recording fillTempDirectory(int count) throws IOException {
        File f = null;
        for (int i = 0; i < count; i++) {
            f = tempFolder.newFile(i + ".wav");
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            out.write(FAKE_DATA);
            out.close();
        }
        return new Recording(f);
    }

}
