package com.soundcloud.android.objects;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class TrackTests {
    @Test
    public void shouldFilterOutMachineTags() throws Exception {
        Track t = new Track();
        t.tag_list = "soundcloud:source=web-record jazz geo:lat=10.23 geo:long=32.232 punk";
        assertThat(t.humanTags(), equalTo(asList("jazz", "punk")));
    }

    @Test
    public void shouldGenerateTrackInfo() throws Exception {
        Track t = new Track();
        t.title = "My Ttrack";
        t.tag_list = "punk";
        t.description = "Cool track";

        assertThat(t.trackInfo(), equalTo("Cool track<br/><br/>punk<br/><br/>all rights reserved<br/><br/>"));
    }

    @Test
    public void shouldDisplayNiceLicense() throws Exception {
        Track t = new Track();

        assertThat(t.license(), equalTo("all rights reserved"));

        t.license = "cc-by-nd";
        assertThat(t.license(), equalTo("attribution no derivative work"));

        t.license = "no-rights-reserved";
        assertThat(t.license(), equalTo("no rights reserved"));
    }
}
