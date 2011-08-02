package com.soundcloud.android.model;

import com.soundcloud.android.provider.DatabaseHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import junit.framework.Assert;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

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
        t.title = "My Ttrack";
        t.tag_list = "punk";
        t.description = "Cool track";

        assertThat(t.trackInfo(), equalTo("Cool track<br/><br/>punk<br/><br/><br/><br/>"));
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
    public void shouldHaveCacheFile() throws Exception {
        Track t = new Track();
        t.id = 10;
        t.getCache().deleteOnExit();

        assertThat(t.getCache().getAbsolutePath().contains(
                "Android/data/com.soundcloud.android/files/.s/d3d9446802a44259755d38e6d163e820")
                , is(true));

        assertThat(t.getCache().exists(), is(false));
        assertThat(t.touchCache(), is(false));
    }

    @Test
    public void shouldDeleteCacheFile() throws Exception {
        Track t = new Track();
        t.id = 10;
        t.getCache().deleteOnExit();

        assertThat(t.getCache().exists(), is(false));
        assertThat(t.createCache(), is(true));
        assertThat(t.getCache().exists(), is(true));
        assertThat(t.deleteCache(), is(true));
        assertThat(t.getCache().exists(), is(false));
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
}
