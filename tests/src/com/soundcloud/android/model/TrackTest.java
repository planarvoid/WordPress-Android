package com.soundcloud.android.model;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

import com.soundcloud.android.provider.DatabaseHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.runner.RunWith;

import android.content.ContentValues;

@RunWith(DefaultTestRunner.class)
public class TrackTest {
    @Test
    public void shouldFilterOutMachineTags() throws Exception {
        Track t = new Track();
        t.tag_list = "soundcloud:source=web-record jazz geo:lat=10.23 geo:long=32.232 punk";
        assertThat(t.humanTags(), equalTo(asList("jazz", "punk")));
    }

    @Test
    public void shouldGenerateTrackInfo() throws Exception {
        Track t = new Track();
        t.description = "Cool track";
        assertThat(t.trackInfo(), equalTo("Cool track<br/><br/>"));
    }

    @Test
    public void shouldAddLineBreaksToTrackInfo() throws Exception {
        Track t = new Track();
        t.description = "Cool\ntrack";
        assertThat(t.trackInfo(), equalTo("Cool<br/>track<br/><br/>"));
    }

    @Test
    public void shouldNotShowAllRightsReserved() throws Exception {
        Track t = new Track();
        assertThat(t.formattedLicense(), equalTo(""));
        t.license = "all-rights-reserved";
        assertThat(t.formattedLicense(), equalTo(""));
    }

    @Test
    public void shouldDisplayNiceCCLicensesWithLinks() throws Exception {
        Track t = new Track();
        t.license = "cc-by-nd";
        assertThat(t.formattedLicense(), equalTo("Licensed under a Creative Commons License " +
                "(<a href='http://creativecommons.org/licenses/by-nd/3.0'>BY-ND</a>)"));

        t.license = "no-rights-reserved";
        assertThat(t.formattedLicense(), equalTo("No Rights Reserved"));
    }

    @Test
    public void shouldShowBpm() throws Exception {
        Track t = new Track();
        t.bpm = 122.3f;

        assertThat(t.trackInfo(), containsString("122.3 BPM"));

        t.bpm = 122.0f;
        assertThat(t.trackInfo(), containsString("122 BPM"));
    }


    @Test
    public void shouldDisplayRecordWith() throws Exception {
        Track t = new Track();
        t.created_with = new Track.CreatedWith();
        t.created_with.name = "FooMaster 3000";
        t.created_with.permalink_url = "http://foomaster.com/";

        assertThat(t.trackInfo(),
                containsString("Created with <a href=\"http://foomaster.com/\">FooMaster 3000</a>"));
    }

    @Test
    public void shouldBuildContentValuesEmpty() throws Exception{
        Track t = new Track();
        ContentValues v = t.buildContentValues();
        assertNotNull(v);
    }

    @Test
    public void shouldBuildContentValuesWithContent() throws Exception{
        Track t = new Track();
        t.id = 1000;
        ContentValues v = t.buildContentValues();
        assertNotNull(v);
        assertThat(v.getAsLong(DatabaseHelper.Tracks.ID), is(1000L));
    }

    @Test
    public void shouldGeneratePageTrack() throws Exception {
        Track t = new Track();
        User u = new User();
        u.permalink = "user";
        t.permalink = "foo";
        t.user = u;
        assertThat(t.pageTrack(), equalTo("/user/foo"));
        assertThat(t.pageTrack("bar"), equalTo("/user/foo/bar"));
        assertThat(t.pageTrack("bar", "baz"), equalTo("/user/foo/bar/baz"));
    }

    @Test
    public void testHasAvatar() throws Exception {
        Track t = new Track();
        assertFalse(t.hasAvatar());
        t.user = new User();
        t.user.avatar_url = "";
        assertFalse(t.hasAvatar());
        t.user.avatar_url = "http://foo.com";
        assertTrue(t.hasAvatar());
    }
}
