package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class TrackTest {
    @Test
    public void shouldFilterOutMachineTags() throws Exception {
        Track t = new Track();
        t.tag_list = "soundcloud:source=web-record jazz geo:lat=10.23 geo:long=32.232 punk";
        expect(t.humanTags()).toContainInOrder("jazz", "punk");
    }

    @Test
    public void shouldHandleMultiWordTags() throws Exception {
        Track t = new Track();
        t.tag_list = "\"multiword tags\" \"in the api\" suck bigtime";
        expect(t.humanTags()).toContainInOrder("multiword tags", "in the api", "suck", "bigtime");
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
        expect(v.getAsLong(DBHelper.Sounds._ID)).toEqual(1000L);
    }

    @Test
    public void shouldBuildContentValuesWithNoLastUpdated() throws Exception{
        Track t = new Track();
        t.id = 1000;
        ContentValues v = t.buildContentValues();
        expect(v.get(DBHelper.Sounds.LAST_UPDATED)).toBeNull();
        t.created_at = new Date(System.currentTimeMillis());
        v = t.buildContentValues();
        expect(v.get(DBHelper.Sounds.LAST_UPDATED)).toBeNull();
        t.duration = 1000;
        v = t.buildContentValues();
        expect(v.get(DBHelper.Sounds.LAST_UPDATED)).toBeNull();
        t.state = Track.State.FINISHED;
        v = t.buildContentValues();
        expect(v.get(DBHelper.Sounds.LAST_UPDATED)).not.toBeNull();
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
        expect(Track.fromIntent(i)).toEqual(t);
    }

    @Test
    public void shouldGetTrackFromIntentTrackCache() throws Exception {
        Track t  = new Track();
        t.id = 0;
        t.permalink = "permalink";
        Intent i = new Intent();
        i.putExtra("track_id", t.id);
        SoundCloudApplication.MODEL_MANAGER.cache(t);
        expect(Track.fromIntent(i)).toEqual(t);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNoIntentPassed() throws Exception {
        Track.fromIntent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfEmptyIntentPassed() throws Exception {
        Track.fromIntent(new Intent());
    }

    @Test
    public void shouldGetArtworkUrl() throws Exception {
        expect(new Track().getArtwork()).toBeNull();

        Track t = new Track();
        t.artwork_url = "http://foo.com/artwork.jpg";
        expect(t.getArtwork()).toEqual("http://foo.com/artwork.jpg");


        Track t2 = new Track();
        t2.user = new User();
        t2.user.avatar_url = "http://avatar.com";
        expect(t2.getArtwork()).toEqual("http://avatar.com");
    }

    @Test
    public void shouldGenerateShareIntentForPublicTrack() throws Exception {
        Track t = new Track();
        t.sharing = Sharing.PUBLIC;
        t.title = "A track";
        t.permalink_url = "http://soundcloud.com/foo/bar";
        Intent intent = t.getShareIntent();
        expect(intent).not.toBeNull();
        expect(intent.getType()).toEqual("text/plain");
        expect(intent.getAction()).toEqual(Intent.ACTION_SEND);
        expect(intent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual("A track on SoundCloud");
        expect(intent.getStringExtra(Intent.EXTRA_TEXT)).toEqual(t.permalink_url);
    }

    @Test
    public void shouldNotGenerateShareIntentForPrivateTrack() throws Exception {
        Track t = new Track();
        Intent intent = t.getShareIntent();
        expect(intent).toBeNull();
    }

    @Test
    public void testShouldIconLoad() throws Exception {
        Track t = new Track();
        expect(t.shouldLoadIcon()).toBeFalse();
        t.artwork_url = "";
        expect(t.shouldLoadIcon()).toBeFalse();
        t.artwork_url = "NULL";
        expect(t.shouldLoadIcon()).toBeFalse();
        t.artwork_url = "http://foo.com";
        expect(t.shouldLoadIcon()).toBeTrue();
    }

    @Test
    public void shouldGetEstimatedFileSize() throws Exception {
        Track t = new Track();
        expect(t.getEstimatedFileSize()).toEqual(0);
        t.duration = 100;
        expect(t.getEstimatedFileSize()).toEqual(1638400);
    }

    @Test
    public void shouldGetUserTrackPermalink() throws Exception {
        Track t = new Track();
        expect(t.userTrackPermalink()).toBeNull();
        t.permalink = "foo";
        expect(t.userTrackPermalink()).toEqual("foo");

        t.user = new User();
        expect(t.userTrackPermalink()).toEqual("foo");

        t.user.permalink = "";
        expect(t.userTrackPermalink()).toEqual("foo");

        t.user.permalink = "user";
        expect(t.userTrackPermalink()).toEqual("user/foo");
    }

    @Test
    public void shouldPersistAndLoadCorrectly() throws Exception {

        DefaultTestRunner.application.setCurrentUserId(100L);
        final ContentResolver resolver = DefaultTestRunner.application.getContentResolver();

        Track t = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("track.json"),
                Track.class);

        Uri uri = SoundCloudApplication.MODEL_MANAGER.write(t);
        expect(uri).not.toBeNull();

        Cursor cursor = resolver.query(uri, null, null, null, null);
        expect(cursor).not.toBeNull();
        expect(cursor.getCount()).toEqual(1);
        expect(cursor.moveToFirst()).toBeTrue();

        Track t2 = new Track(cursor);
        compareTracks(t, t2);
        expect(t2.last_updated).toBeGreaterThan(t.last_updated);
        expect(t2.sharing).toEqual(t.sharing);
        expect(t2.state).toEqual(t.state);
    }

    @Test
    public void shouldParcelAndUnparcelCorrectly() throws Exception {
        Track t = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("track.json"),
                Track.class);

        Parcel p = Parcel.obtain();
        t.writeToParcel(p, 0);

        Track t2 = new Track(p);
        compareTracks(t, t2);
    }

    @Test
    public void shouldGetWaveformDataURL() throws Exception {
        Track t = new Track();
        expect(t.getWaveformDataURL()).toBeNull();
        t.waveform_url = "http://waveforms.soundcloud.com/bypOn0pnRvFf_m.png";
        expect(t.getWaveformDataURL().toString()).toEqual("http://wis.sndcdn.com/bypOn0pnRvFf_m.png");
    }

    private void compareTracks(Track t, Track t2) {
        expect(t2.id).toEqual(t.id);
        expect(t2.title).toEqual(t.title);
        expect(t2.permalink).toEqual(t.permalink);
        expect(t2.duration).toBeGreaterThan(0);
        expect(t2.duration).toEqual(t.duration);
        expect(t2.created_at).toEqual(t.created_at);
        expect(t2.tag_list).toEqual(t.tag_list);
        expect(t2.track_type).toEqual(t.track_type);
        expect(t2.permalink_url).toEqual(t.permalink_url);
        expect(t2.artwork_url).toEqual(t.artwork_url);
        expect(t2.waveform_url).toEqual(t.waveform_url);
        expect(t2.downloadable).toEqual(t.downloadable);
        expect(t2.download_url).toEqual(t.download_url);
        expect(t2.streamable).toEqual(t.streamable);
        expect(t2.stream_url).toEqual(t.stream_url);
        expect(t2.playback_count).toEqual(t.playback_count);
        expect(t2.download_count).toEqual(t.download_count);
        expect(t2.comment_count).toEqual(t.comment_count);
        expect(t2.likes_count).toEqual(t.likes_count);
        expect(t2.shared_to_count).toEqual(t.shared_to_count);
        expect(t2.user_id).toEqual(t.user_id);
        expect(t2.commentable).toEqual(t.commentable);
    }
}
