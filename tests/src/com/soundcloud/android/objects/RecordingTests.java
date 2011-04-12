package com.soundcloud.android.objects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import com.soundcloud.api.CloudAPI;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


@RunWith(RobolectricTestRunner.class)
public class RecordingTests {

    Recording r;


    @Before
    public void setup() throws Exception {
        r = new Recording();
        // 14:31:01, 15/02/2011
        Calendar c = Calendar.getInstance();
        c.set(2001, 1, 15, 14, 31, 1);
        r.timestamp = c.getTimeInMillis();
        r.service_ids = "1,2,3";
        r.audio_path  = "/tmp/foo";

    }

    @Test
    public void itShouldHaveANiceSharingNote() throws Exception {
        r.prepareForUpload();
        String note = String.valueOf(r.upload_data.get(CloudAPI.TrackParams.SHARING_NOTE));
        assertThat(note, equalTo("Sounds from Thursday afternoon"));
    }

    @Test
    public void shouldGenerateASharingNoteWithLocation() throws Exception {
        r.where_text = "Mars";
        r.prepareForUpload();
        String note = String.valueOf(r.upload_data.get(CloudAPI.TrackParams.SHARING_NOTE));
        assertThat(note, equalTo("Sounds from Mars"));
    }

    @Test
    public void shouldGenerateASharingNoteWithLocationAndTitle() throws Exception {
        r.what_text = "Party";
        r.where_text = "Mars";
        r.prepareForUpload();
        String note = String.valueOf(r.upload_data.get(CloudAPI.TrackParams.SHARING_NOTE));
        assertThat(note, equalTo("Party at Mars"));
    }

    @Test
    public void shouldGenerateASharingNoteWithTitle() throws Exception {
        r.what_text = "Party";
        r.prepareForUpload();
        String note = String.valueOf(r.upload_data.get(CloudAPI.TrackParams.SHARING_NOTE));
        assertThat(note, equalTo("Party"));
    }

    @Test
    public void shouldSetADifferentMachineTagWhenDoing3rdPartyUpload() throws Exception {
        r.external_upload = true;
        r.prepareForUpload();
        String tags = String.valueOf(r.upload_data.get(CloudAPI.TrackParams.TAG_LIST));
        List<String> tagList = Arrays.asList(tags.split("\\s+"));
        assertThat(tagList, hasItem("soundcloud:source=android-3rdparty-upload"));
    }
}
