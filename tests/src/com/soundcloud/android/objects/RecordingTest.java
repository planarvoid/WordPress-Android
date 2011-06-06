package com.soundcloud.android.objects;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Params;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


@RunWith(DefaultTestRunner.class)
public class RecordingTest {
    Recording r;

    @Before
    public void setup() throws Exception {
        r = new Recording(new File("/tmp/foo"));
        // 14:31:01, 15/02/2011
        Calendar c = Calendar.getInstance();
        c.set(2001, 1, 15, 14, 31, 1);
        r.timestamp = c.getTimeInMillis();
        r.service_ids = "1,2,3";
        r.duration = 86 * 1000;
    }

    @Test
    public void itShouldHaveANiceSharingNote() throws Exception {
        assertThat(r.sharingNote(), equalTo("Sounds from Thursday afternoon"));
    }

    @Test
    public void shouldGenerateASharingNoteWithLocation() throws Exception {
        r.where_text = "Mars";
        assertThat(r.sharingNote(), equalTo("Sounds from Mars"));
    }

    @Test
    public void shouldGenerateASharingNoteWithLocationAndTitle() throws Exception {
        r.what_text = "Party";
        r.where_text = "Mars";
        assertThat(r.sharingNote(), equalTo("Party at Mars"));
    }

    @Test
    public void shouldGenerateASharingNoteWithTitle() throws Exception {
        r.what_text = "Party";
        assertThat(r.sharingNote(), equalTo("Party"));
    }

    @Test
    public void shouldSetADifferentMachineTagWhenDoing3rdPartyUpload() throws Exception {
        r.external_upload = true;
        r.prepareForUpload();
        String tags = String.valueOf(r.upload_data.get(Params.Track.TAG_LIST));
        List<String> tagList = Arrays.asList(tags.split("\\s+"));
        assertThat(tagList.contains("soundcloud:source=android-3rdparty-upload"), is(true));
    }

    @Test
    public void shouldGenerateStatusWithNotUploaded() throws Exception {
        assertThat(
                r.getStatus(Robolectric.application.getResources()),
                equalTo("null, 1.26, not yet uploaded"));
    }

    @Test
     public void shouldGenerateStatusWithError() throws Exception {
        r.upload_error = true;
        assertThat(
                 r.getStatus(Robolectric.application.getResources()),
                 equalTo("null, 1.26, upload failed"));
     }

    @Test
    public void shouldGenerateStatusWithCurrentlyUploading() throws Exception {
        r.upload_status = 1;
        assertThat(
                r.getStatus(Robolectric.application.getResources()),
                equalTo("Uploading, progress is in notifications"));
    }

    @Test
    public void shouldHaveFormattedDuration() throws Exception {
        assertThat(r.formattedDuration(), equalTo("1.26"));
    }
}
