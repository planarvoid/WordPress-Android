package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.content.Intent;

import java.util.*;

@RunWith(DefaultTestRunner.class)
public class TrackTest {
    @Test
    public void shouldFilterOutMachineTags() throws Exception {
        Track t = new Track();
        t.tag_list = "soundcloud:source=web-record jazz geo:lat=10.23 geo:long=32.232 punk";
        expect(t.humanTags()).toContainInOrder("jazz", "punk");
    }

    @Test
    public void shouldGenerateTrackInfo() throws Exception {
        Track t = new Track();
        t.description = "Cool track";
        expect(t.trackInfo()).toEqual("Cool track<br/><br/>");
    }

    @Test
    public void shouldAddLineBreaksToTrackInfo() throws Exception {
        Track t = new Track();
        t.description = "Cool\ntrack";
        expect(t.trackInfo()).toEqual("Cool<br/>track<br/><br/>");
    }

    @Test
    public void shouldNotShowAllRightsReserved() throws Exception {
        Track t = new Track();
        expect(t.formattedLicense()).toEqual("");
        t.license = "all-rights-reserved";
        expect(t.formattedLicense()).toEqual("");
    }

    @Test
    public void shouldDisplayNiceCCLicensesWithLinks() throws Exception {
        Track t = new Track();
        t.license = "cc-by-nd";
        expect(t.formattedLicense()).toEqual("Licensed under a Creative Commons License " +
                "(<a href='http://creativecommons.org/licenses/by-nd/3.0'>BY-ND</a>)");

        t.license = "no-rights-reserved";
        expect(t.formattedLicense()).toEqual("No Rights Reserved");
    }

    @Test
    public void shouldShowBpm() throws Exception {
        Track t = new Track();
        t.bpm = 122.3f;

        expect(t.trackInfo()).toContain("122.3 BPM");

        t.bpm = 122.0f;
        expect(t.trackInfo()).toContain("122 BPM");
    }


    @Test
    public void shouldDisplayRecordWith() throws Exception {
        Track t = new Track();
        t.created_with = new Track.CreatedWith();
        t.created_with.name = "FooMaster 3000";
        t.created_with.permalink_url = "http://foomaster.com/";

        expect(t.trackInfo()).toContain("Created with <a href=\"http://foomaster.com/\">FooMaster 3000</a>");
    }

    @Test
    public void shouldBuildContentValuesEmpty() throws Exception{
        Track t = new Track();
        ContentValues v = t.buildContentValues();
        expect(v).not.toBeNull();
    }

    @Test
    public void shouldBuildContentValuesWithContent() throws Exception{
        Track t = new Track();
        t.id = 1000;
        ContentValues v = t.buildContentValues();
        expect(v).not.toBeNull();
        expect(v.getAsLong(DBHelper.Tracks._ID)).toEqual(1000L);
    }

    @Test
    public void shouldBuildContentValuesWithNoLastUpdated() throws Exception{
        Track t = new Track();
        t.id = 1000;
        ContentValues v = t.buildContentValues();
        expect(v.get(DBHelper.Tracks.LAST_UPDATED)).toBeNull();
        t.created_at = new Date(System.currentTimeMillis());
        v = t.buildContentValues();
        expect(v.get(DBHelper.Tracks.LAST_UPDATED)).toBeNull();
        t.duration = 1000;
        v = t.buildContentValues();
        expect(v.get(DBHelper.Tracks.LAST_UPDATED)).not.toBeNull();
    }

    @Test
    public void shouldGeneratePageTrack() throws Exception {
        Track t = new Track();
        User u = new User();
        u.permalink = "user";
        t.permalink = "foo";
        t.user = u;
        expect(t.pageTrack()).toEqual("/user/foo");
        expect(t.pageTrack("bar")).toEqual("/user/foo/bar");
        expect(t.pageTrack("bar", "baz")).toEqual("/user/foo/bar/baz");
    }

    @Test
    public void testHasAvatar() throws Exception {
        Track t = new Track();
        expect(t.hasAvatar()).toBeFalse();
        t.user = new User();
        t.user.avatar_url = "";
        expect(t.hasAvatar()).toBeFalse();
        t.user.avatar_url = "http://foo.com";
        expect(t.hasAvatar()).toBeTrue();
    }

    @Test
    public void shouldGetTrackFromIntentParcelable() throws Exception {
        Track t  = new Track();
        t.id = 0;
        t.permalink = "permalink";
        Intent i = new Intent();
        i.putExtra("track", t);
        expect(Track.fromIntent(i, null)).toEqual(t);
    }

    @Test
    public void shouldGetTrackFromIntentTrackCache() throws Exception {
        Track t  = new Track();
        t.id = 0;
        t.permalink = "permalink";
        Intent i = new Intent();
        i.putExtra("track_id", t.id);
        SoundCloudApplication.TRACK_CACHE.put(t);
        expect(Track.fromIntent(i, null)).toEqual(t);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNoIntentPassed() throws Exception {
        Track.fromIntent(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfEmptyIntentPassed() throws Exception {
        Track.fromIntent(new Intent(), null);
    }
}
