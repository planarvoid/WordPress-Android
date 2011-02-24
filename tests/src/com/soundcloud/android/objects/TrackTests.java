package com.soundcloud.android.objects;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

public class TrackTests {
    @Test
    public void shouldFilterOutMachineTags() throws Exception {
        Track t = new Track();
        t.tag_list = "soundcloud:source=web-record jazz geo:lat=10.23 geo:long=32.232 punk";
        assertThat(t.humanTags(), equalTo(asList("jazz", "punk")));
    }
}
