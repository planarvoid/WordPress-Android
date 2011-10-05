package com.soundcloud.android.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.record.CloudRecorder;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.res.Resources;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;


@SuppressWarnings({"ResultOfMethodCallIgnored"})
@RunWith(DefaultTestRunner.class)
public class RecordingTest {
    Recording r;
    File f;
    Resources res;

    @Before
    public void setup() throws Exception {
        f = new File("/tmp/recording-test");
        r = new Recording(f);
        if (f.exists()) {
            assertThat(f.delete(), is(true));
        }
        // 14:31:01, 15/02/2011
        Calendar c = Calendar.getInstance();
        c.set(2001, 1, 15, 14, 31, 1);
        r.timestamp = c.getTimeInMillis();
        r.service_ids = "1,2,3";
        r.duration = 86 * 1000;
        res = Robolectric.application.getResources();
    }

    @Test
    public void itShouldHaveANiceSharingNote() throws Exception {
        assertThat(r.sharingNote(res), equalTo("Sounds from Thursday afternoon"));
    }

    @Test
    public void shouldGenerateASharingNoteWithLocation() throws Exception {
        r.where_text = "Mars";
        assertThat(r.sharingNote(res), equalTo("Sounds from Mars"));
    }

    @Test
    public void shouldGenerateASharingNoteWithLocationAndTitle() throws Exception {
        r.what_text = "Party";
        r.where_text = "Mars";
        assertThat(r.sharingNote(res), equalTo("Party at Mars"));
    }

    @Test
    public void shouldGenerateASharingNoteWithTitle() throws Exception {
        r.what_text = "Party";
        assertThat(r.sharingNote(res), equalTo("Party"));
    }


    @Test
    public void shouldGenerateStatusWithNotUploaded() throws Exception {
        assertThat(
                r.getStatus(res),
                equalTo("10 years, 1.26, not yet uploaded"));
    }

    @Test
     public void shouldGenerateStatusWithError() throws Exception {
        r.upload_error = true;
        assertThat(
                 r.getStatus(res),
                 equalTo("10 years, 1.26, upload failed"));
     }

    @Test
    public void shouldGenerateStatusWithCurrentlyUploading() throws Exception {
        r.upload_status = 1;
        assertThat(
                r.getStatus(res),
                equalTo("Uploading. You can check on progress in Notifications"));
    }

    @Test
    public void shouldHaveFormattedDuration() throws Exception {
        assertThat(r.formattedDuration(), equalTo("1.26"));
    }

    @Test
    public void shouldGenerateAnUploadFilename() throws Exception {
        assertThat(r.generateUploadFilename("A Title").getAbsolutePath(),
                equalTo("/tmp/A_Title_2001-02-15-02-31-01.mp4"));

        r.audio_profile = CloudRecorder.Profile.RAW;
        assertThat(r.generateUploadFilename("A Title").getAbsolutePath(),
                equalTo("/tmp/.encode/A_Title_2001-02-15-02-31-01.ogg"));
    }


    @Test
    public void shouldDeleteRecording() throws Exception {
        assertThat(r.delete(null), is(false));
        assertThat(f.createNewFile(), is(true));
        assertThat(r.delete(null),( is(true)));
        assertThat(f.exists(), is(false));
    }

    @Test
    public void shouldNotDeleteRecordingIfExternal() throws Exception {
        r.external_upload = true;
        assertThat(f.createNewFile(), is(true));
        assertThat(r.delete(null),( is(false)));
        assertThat(f.exists(), is(true));
    }

    @Test
    public void shouldGenerateImageFilename() throws Exception {
        assertThat(new Recording(new File("/tmp/foo.mp4")).generateImageFile(new File("/images")).getAbsolutePath(),
                equalTo("/images/foo.bmp"));

        assertThat(new Recording(new File("/tmp/foo")).generateImageFile(new File("/images")).getAbsolutePath(),
                equalTo("/images/foo.bmp"));
    }

    @Test
    public void shouldGeneratePageTrack() throws Exception {
        Recording r = new Recording(new File("/tmp"));
        assertThat(r.pageTrack(), equalTo("/record/share/public"));
        r.is_private = true;
        assertThat(r.pageTrack(), equalTo("/record/share/private"));
    }
}
