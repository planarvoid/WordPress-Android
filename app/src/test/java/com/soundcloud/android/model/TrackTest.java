package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.playback.streaming.StreamItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Parcel;
import android.text.TextUtils;

import java.io.IOException;
import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class TrackTest {

    @Test
    public void shouldConstructTrackFromId() {
        Track t = new Track(1L);
        expect(t.getUrn().toString()).toEqual("soundcloud:sounds:1");
        expect(t.getId()).toEqual(1L);
    }

    @Test
    public void setIdShouldUpdateUrn() throws Exception {
        Track t = new Track();
        t.setId(1000L);
        expect(t.getUrn().toString()).toEqual("soundcloud:sounds:1000");
    }

    @Test
    public void setUrnShouldUpdateId() throws Exception {
        Track t = new Track();
        t.setUrn("soundcloud:sounds:1000");
        expect(t.getId()).toEqual(1000L);
    }

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
    public void getGenreOrFirstTagShouldReturnNullIfGenreEmptyAndHumanTagsMissing(){
        expect(new Track().getGenreOrTag()).toBeNull();
    }

    @Test
    public void getGenreOrFirstTagShouldReturnGenreIfNotEmpty() throws Exception {
        Track t = new Track();
        t.genre = "some genre";
        t.tag_list = "\"multiword tags\" \"in the api\" suck bigtime";
        expect(t.getGenreOrTag()).toEqual("some genre");
    }

    @Test
    public void getGenreOrFirstTagShouldReturnFirstTagIfGenreEmptyAndHumanTagsNotEmpty() throws Exception {
        Track t = new Track();
        t.tag_list = "\"multiword tags\" \"in the api\" suck bigtime";
        expect(t.getGenreOrTag()).toEqual("multiword tags");
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
        t.setId(1000);
        ContentValues v = t.buildContentValues();
        expect(v).not.toBeNull();
        expect(v.getAsLong(TableColumns.Sounds._ID)).toEqual(1000L);
    }

    @Test
    public void shouldBuildContentValuesWithNoLastUpdated() throws Exception{
        Track t = new Track();
        t.setId(1000);
        ContentValues v = t.buildContentValues();
        expect(v.get(TableColumns.Sounds.LAST_UPDATED)).toBeNull();
        t.created_at = new Date(System.currentTimeMillis());
        v = t.buildContentValues();
        expect(v.get(TableColumns.Sounds.LAST_UPDATED)).toBeNull();
        t.duration = 1000;
        v = t.buildContentValues();
        expect(v.get(TableColumns.Sounds.LAST_UPDATED)).toBeNull();
        t.state = Track.State.FINISHED;
        v = t.buildContentValues();
        expect(v.get(TableColumns.Sounds.LAST_UPDATED)).not.toBeNull();
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
        expect(t.shouldLoadArtwork()).toBeFalse();
        t.artwork_url = "";
        expect(t.shouldLoadArtwork()).toBeFalse();
        t.artwork_url = "NULL";
        expect(t.shouldLoadArtwork()).toBeFalse();
        t.artwork_url = "http://foo.com";
        expect(t.shouldLoadArtwork()).toBeTrue();
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

    @Test
    public void shouldAppendTrackIdToStreamUrl() throws Exception {
        Track t = new Track(123L);
        t.stream_url = "http://media.soundcloud.com/stream/tfmLdABNn0wb";
        expect(t.getStreamUrlWithAppendedId().toString()).toEqual("http://media.soundcloud.com/stream/tfmLdABNn0wb?" + StreamItem.TRACK_ID_KEY + "=123");
    }

    @Test
    public void shouldCreateTrackFromTrackSummary() throws IOException {
        TrackSummary suggestion = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), TrackSummary.class);
        Track t = new Track(suggestion);

        expect(t.getUrn()).toEqual(suggestion.getUrn());
        expect(t.getUser().getUsername()).toEqual(suggestion.getUser().getUsername());
        expect(t.getUser().getUrn()).toEqual(suggestion.getUser().getUrn());
        expect(t.getTitle()).toEqual(suggestion.getTitle());
        expect(t.getArtwork()).toEqual(suggestion.getArtworkUrl());
        expect(t.genre).toEqual(suggestion.getGenre());
        expect(t.commentable).toEqual(suggestion.isCommentable());
        expect(t.stream_url).toEqual(suggestion.getStreamUrl());
        expect(t.waveform_url).toEqual(Track.fixWaveform(suggestion.getWaveformUrl()));
        expect(t.tag_list).toEqual(TextUtils.join(" ", suggestion.getUserTags()));
        expect(t.created_at).toEqual(suggestion.getCreatedAt());
        expect(t.duration).toEqual(suggestion.getDuration());
        expect(t.sharing).toEqual(suggestion.getSharing());
        expect(t.permalink_url).toEqual(suggestion.getPermalinkUrl());

        expect(t.likes_count).toEqual(suggestion.getStats().getLikesCount());
        expect(t.playback_count).toEqual(suggestion.getStats().getPlaybackCount());
        expect(t.reposts_count).toEqual(suggestion.getStats().getRepostsCount());
        expect(t.comment_count).toEqual(suggestion.getStats().getCommentsCount());
    }

    @Test
    public void shouldConvertToPropertySet() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);

        PropertySet propertySet = track.toPropertySet();
        expect(propertySet.get(PlayableProperty.DURATION)).toEqual(track.duration);
        expect(propertySet.get(PlayableProperty.TITLE)).toEqual(track.title);
        expect(propertySet.get(PlayableProperty.URN)).toEqual(track.getUrn());
        expect(propertySet.get(PlayableProperty.CREATOR_URN)).toEqual(track.getUser().getUrn());
        expect(propertySet.get(PlayableProperty.CREATOR_NAME)).toEqual(track.getUsername());
        expect(propertySet.get(PlayableProperty.IS_PRIVATE)).toEqual(track.isPrivate());
        expect(propertySet.get(TrackProperty.PLAY_COUNT)).toEqual(track.playback_count);
    }

    @Test
    public void shouldConvertToPropertySetWithBlankUsernameIfUsernameNull() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        track.setUser(new User());
        PropertySet propertySet = track.toPropertySet();
        expect(propertySet.get(PlayableProperty.CREATOR_NAME)).toEqual(ScTextUtils.EMPTY_STRING);
    }

    private void compareTracks(Track t, Track t2) {
        expect(t2.getId()).toEqual(t.getId());
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
