package com.soundcloud.android.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
public class UploadTest {
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

    private Upload upload() {
        return new Upload(r, Robolectric.application.getResources());
    }

    @Test
    public void shouldAddTagsToUploadData() throws Exception {
        r.tags = new String[] { "foo baz", "bar", "baz" };
        String tags = String.valueOf(upload().tag_list);
        assertThat(tags, equalTo("\"foo baz\" bar baz soundcloud:source=android-record"));
    }

    @Test
    public void shouldAddDescriptionToUploadData() throws Exception {
        r.description = "foo";
        assertThat(upload().toTrackMap().get(Params.Track.DESCRIPTION).toString(), equalTo("foo"));
    }

    @Test
    public void shouldAddGenreToUploadData() throws Exception {
        r.genre = "foo";
        assertThat(upload().toTrackMap().get(Params.Track.GENRE).toString(), equalTo("foo"));
    }

    @Test
    public void shouldAddFoursquareMachineTags() throws Exception {
        r.four_square_venue_id = "abcdef";
        assertTrue(upload().getTags().contains("foursquare:venue=abcdef"));
    }

    @Test
    public void shouldSetADifferentMachineTagWhenDoing3rdPartyUpload() throws Exception {
        r.external_upload = true;
        String tags = String.valueOf(upload().toTrackMap().get(Params.Track.TAG_LIST));
        List<String> tagList = Arrays.asList(tags.split("\\s+"));
        assertThat(tagList.contains("soundcloud:source=android-3rdparty-upload"), is(true));
    }

    @Test
    public void shouldSetSourceMachineTag() throws Exception {
        assertThat(upload().getTags().contains("soundcloud:source=android-record"), is(true));
    }

    @Test
    public void shouldSetGeoMachineTags() throws Exception {
        r.longitude = 0.1d;
        r.latitude = 0.2d;
        List<String> tags = upload().getTags();
        assertThat(tags.contains("geo:lon=0.1"), is(true));
        assertThat(tags.contains("geo:lat=0.2"), is(true));
    }
}
