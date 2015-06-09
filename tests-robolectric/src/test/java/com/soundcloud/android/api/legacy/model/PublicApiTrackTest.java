package com.soundcloud.android.api.legacy.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.os.Parcel;
import android.text.TextUtils;

import java.io.IOException;
import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class PublicApiTrackTest {

    @Test
    public void shouldConstructTrackFromId() {
        PublicApiTrack t = new PublicApiTrack(1L);

        expect(t.getUrn().toString()).toEqual("soundcloud:tracks:1");
        expect(t.getId()).toEqual(1L);
    }

    @Test
    public void setIdShouldUpdateUrn() {
        PublicApiTrack t = new PublicApiTrack();
        t.setId(1000L);

        expect(t.getUrn().toString()).toEqual("soundcloud:tracks:1000");
    }

    @Test
    public void setUrnShouldUpdateId() {
        PublicApiTrack t = new PublicApiTrack();
        t.setUrn("soundcloud:tracks:1000");

        expect(t.getId()).toEqual(1000L);
    }

    @Test
    public void shouldFilterOutMachineTags() {
        PublicApiTrack t = new PublicApiTrack();
        t.tag_list = "soundcloud:source=web-record jazz geo:lat=10.23 geo:long=32.232 punk";

        expect(t.humanTags()).toContainInOrder("jazz", "punk");
    }

    @Test
    public void shouldHandleMultiWordTags() {
        PublicApiTrack t = new PublicApiTrack();
        t.tag_list = "\"multiword tags\" \"in the api\" suck bigtime";

        expect(t.humanTags()).toContainInOrder("multiword tags", "in the api", "suck", "bigtime");
    }

    @Test
    public void getGenreOrFirstTagShouldReturnNullIfGenreEmptyAndHumanTagsMissing() {
        expect(new PublicApiTrack().getGenreOrTag()).toBeNull();
    }

    @Test
    public void getGenreOrFirstTagShouldReturnGenreIfNotEmpty() {
        PublicApiTrack t = new PublicApiTrack();
        t.genre = "some genre";
        t.tag_list = "\"multiword tags\" \"in the api\" suck bigtime";

        expect(t.getGenreOrTag()).toEqual("some genre");
    }

    @Test
    public void getGenreOrFirstTagShouldReturnFirstTagIfGenreEmptyAndHumanTagsNotEmpty() {
        PublicApiTrack t = new PublicApiTrack();
        t.tag_list = "\"multiword tags\" \"in the api\" suck bigtime";

        expect(t.getGenreOrTag()).toEqual("multiword tags");
    }

    @Test
    public void shouldGenerateTrackInfo() {
        PublicApiTrack t = new PublicApiTrack();
        t.description = "Cool track";

        expect(t.trackInfo()).toEqual("Cool track<br/><br/>");
    }

    @Test
    public void shouldAddLineBreaksToTrackInfo() {
        PublicApiTrack t = new PublicApiTrack();
        t.description = "Cool\ntrack";

        expect(t.trackInfo()).toEqual("Cool<br/>track<br/><br/>");
    }

    @Test
    public void shouldNotShowAllRightsReserved() {
        PublicApiTrack t = new PublicApiTrack();
        expect(t.formattedLicense()).toEqual("");

        t.license = "all-rights-reserved";
        expect(t.formattedLicense()).toEqual("");
    }

    @Test
    public void shouldDisplayNiceCCLicensesWithLinks() {
        PublicApiTrack t = new PublicApiTrack();
        t.license = "cc-by-nd";
        expect(t.formattedLicense()).toEqual("Licensed under a Creative Commons License " +
                "(<a href='http://creativecommons.org/licenses/by-nd/3.0'>BY-ND</a>)");

        t.license = "no-rights-reserved";
        expect(t.formattedLicense()).toEqual("No Rights Reserved");
    }

    @Test
    public void shouldShowBpm() {
        PublicApiTrack t = new PublicApiTrack();
        t.bpm = 122.3f;

        expect(t.trackInfo()).toContain("122.3 BPM");

        t.bpm = 122.0f;
        expect(t.trackInfo()).toContain("122 BPM");
    }


    @Test
    public void shouldDisplayRecordWith() {
        PublicApiTrack t = new PublicApiTrack();
        t.created_with = new PublicApiTrack.CreatedWith();
        t.created_with.name = "FooMaster 3000";
        t.created_with.permalink_url = "http://foomaster.com/";

        expect(t.trackInfo()).toContain("Created with <a href=\"http://foomaster.com/\">FooMaster 3000</a>");
    }

    @Test
    public void shouldBuildContentValuesEmpty() {
        PublicApiTrack t = new PublicApiTrack();
        ContentValues v = t.buildContentValues();

        expect(v).not.toBeNull();
    }

    @Test
    public void shouldBuildContentValuesWithContent() {
        PublicApiTrack t = new PublicApiTrack();
        t.setId(1000);
        ContentValues v = t.buildContentValues();

        expect(v).not.toBeNull();
        expect(v.getAsLong(TableColumns.Sounds._ID)).toEqual(1000L);
    }

    @Test
    public void shouldBuildContentValuesWithNoLastUpdated() {
        PublicApiTrack t = new PublicApiTrack();
        t.setId(1000);

        ContentValues v = t.buildContentValues();
        expect(v.get(TableColumns.Sounds.LAST_UPDATED)).toBeNull();

        t.created_at = new Date(System.currentTimeMillis());
        v = t.buildContentValues();
        expect(v.get(TableColumns.Sounds.LAST_UPDATED)).toBeNull();

        t.duration = 1000;
        v = t.buildContentValues();
        expect(v.get(TableColumns.Sounds.LAST_UPDATED)).toBeNull();

        t.state = PublicApiTrack.State.FINISHED;
        v = t.buildContentValues();
        expect(v.get(TableColumns.Sounds.LAST_UPDATED)).not.toBeNull();
    }

    @Test
    public void testHasAvatar() {
        PublicApiTrack t = new PublicApiTrack();
        expect(t.hasAvatar()).toBeFalse();

        t.user = new PublicApiUser();
        t.user.avatar_url = "";
        expect(t.hasAvatar()).toBeFalse();

        t.user.avatar_url = "http://foo.com";
        expect(t.hasAvatar()).toBeTrue();
    }

    @Test
    public void shouldGetArtworkUrl() {
        expect(new PublicApiTrack().getArtwork()).toBeNull();

        PublicApiTrack t = new PublicApiTrack();
        t.artwork_url = "http://foo.com/artwork.jpg";
        expect(t.getArtwork()).toEqual("http://foo.com/artwork.jpg");


        PublicApiTrack t2 = new PublicApiTrack();
        t2.user = new PublicApiUser();
        t2.user.avatar_url = "http://avatar.com";
        expect(t2.getArtwork()).toEqual("http://avatar.com");
    }

    @Test
    public void testShouldIconLoad() {
        PublicApiTrack t = new PublicApiTrack();
        expect(t.shouldLoadArtwork()).toBeFalse();

        t.artwork_url = "";
        expect(t.shouldLoadArtwork()).toBeFalse();

        t.artwork_url = "NULL";
        expect(t.shouldLoadArtwork()).toBeFalse();

        t.artwork_url = "http://foo.com";
        expect(t.shouldLoadArtwork()).toBeTrue();
    }

    @Test
    public void shouldGetEstimatedFileSize() {
        PublicApiTrack t = new PublicApiTrack();
        expect(t.getEstimatedFileSize()).toEqual(0);

        t.duration = 100;
        expect(t.getEstimatedFileSize()).toEqual(1638400);
    }

    @Test
    public void shouldGetUserTrackPermalink() {
        PublicApiTrack t = new PublicApiTrack();
        expect(t.userTrackPermalink()).toBeNull();

        t.permalink = "foo";
        expect(t.userTrackPermalink()).toEqual("foo");

        t.user = new PublicApiUser();
        expect(t.userTrackPermalink()).toEqual("foo");

        t.user.permalink = "";
        expect(t.userTrackPermalink()).toEqual("foo");

        t.user.permalink = "user";
        expect(t.userTrackPermalink()).toEqual("user/foo");
    }


    @Test
    public void shouldParcelAndUnparcelCorrectly() throws IOException {
        PublicApiTrack t = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("track.json"),
                PublicApiTrack.class);

        Parcel p = Parcel.obtain();
        t.writeToParcel(p, 0);

        PublicApiTrack t2 = new PublicApiTrack(p);
        compareTracks(t, t2);
    }

    @Test
    public void shouldGetWaveformDataURL() {
        PublicApiTrack t = new PublicApiTrack();
        expect(t.getWaveformDataURL()).toBeNull();
        t.waveform_url = "http://waveforms.soundcloud.com/bypOn0pnRvFf_m.png";
        expect(t.getWaveformDataURL().toString()).toEqual("http://wis.sndcdn.com/bypOn0pnRvFf_m.png");
    }

    @Test
    public void shouldCreateTrackFromTrackSummary() throws IOException {
        ApiTrack suggestion = TestHelper.readJson(ApiTrack.class,
                "/com/soundcloud/android/api/model/suggested_track.json");
        PublicApiTrack t = new PublicApiTrack(suggestion);

        expect(t.getUrn()).toEqual(suggestion.getUrn());
        expect(t.getUser().getUsername()).toEqual(suggestion.getUser().getUsername());
        expect(t.getUser().getUrn()).toEqual(suggestion.getUser().getUrn());
        expect(t.getTitle()).toEqual(suggestion.getTitle());
        expect(t.getArtwork()).toEqual(suggestion.getArtworkUrl());
        expect(t.genre).toEqual(suggestion.getGenre());
        expect(t.commentable).toEqual(suggestion.isCommentable());
        expect(t.stream_url).toEqual(suggestion.getStreamUrl());
        expect(t.waveform_url).toEqual(PublicApiTrack.fixWaveform(suggestion.getWaveformUrl()));
        expect(t.tag_list).toEqual(TextUtils.join(" ", suggestion.getUserTags()));
        expect(t.created_at).toEqual(suggestion.getCreatedAt());
        expect(t.duration).toEqual(suggestion.getDuration());
        expect(t.sharing).toEqual(suggestion.getSharing());
        expect(t.permalink_url).toEqual(suggestion.getPermalinkUrl());
        expect(t.policy).toEqual(suggestion.getPolicy());

        expect(t.likes_count).toEqual(suggestion.getStats().getLikesCount());
        expect(t.playback_count).toEqual(suggestion.getStats().getPlaybackCount());
        expect(t.reposts_count).toEqual(suggestion.getStats().getRepostsCount());
        expect(t.comment_count).toEqual(suggestion.getStats().getCommentsCount());
    }

    @Test
    public void shouldConvertToPropertySet() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);

        PropertySet propertySet = assertTrackPropertiesWithoutPolicyInfo(track);
        expect(propertySet.get(TrackProperty.POLICY)).toEqual(track.policy);
        expect(propertySet.get(TrackProperty.MONETIZABLE)).toEqual(track.isMonetizable());
    }

    @Test
    public void shouldConvertToPropertySetWithNoPolicy() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        track.setPolicy(null);

        PropertySet propertySet = assertTrackPropertiesWithoutPolicyInfo(track);
        expect(propertySet.get(TrackProperty.MONETIZABLE)).toBeFalse();
    }

    private PropertySet assertTrackPropertiesWithoutPolicyInfo(PublicApiTrack track) {
        PropertySet propertySet = track.toPropertySet();
        expect(propertySet.get(PlayableProperty.DURATION)).toEqual(track.duration);
        expect(propertySet.get(PlayableProperty.TITLE)).toEqual(track.title);
        expect(propertySet.get(PlayableProperty.URN)).toEqual(track.getUrn());
        expect(propertySet.get(PlayableProperty.CREATOR_URN)).toEqual(track.getUser().getUrn());
        expect(propertySet.get(PlayableProperty.CREATOR_NAME)).toEqual(track.getUsername());
        expect(propertySet.get(PlayableProperty.IS_PRIVATE)).toEqual(track.isPrivate());
        expect(propertySet.get(TrackProperty.PLAY_COUNT)).toEqual(track.playback_count);
        expect(propertySet.get(PlayableProperty.LIKES_COUNT)).toEqual(track.likes_count);
        expect(propertySet.get(PlayableProperty.REPOSTS_COUNT)).toEqual(track.reposts_count);
        expect(propertySet.get(PlayableProperty.IS_LIKED)).toEqual(track.user_like);
        expect(propertySet.get(PlayableProperty.IS_REPOSTED)).toEqual(track.user_repost);
        expect(propertySet.get(PlayableProperty.CREATED_AT)).toEqual(track.created_at);
        expect(propertySet.get(TrackProperty.COMMENTS_COUNT)).toEqual(track.comment_count);
        return propertySet;
    }

    @Test
    public void shouldConvertToApiMobileTrack(){
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        ApiTrack apiMobileTrack = track.toApiMobileTrack();

        expect(apiMobileTrack.getCommentsCount()).toEqual(track.getCommentsCount());
        expect(apiMobileTrack.getCreatedAt()).toEqual(track.getCreatedAt());
        expect(apiMobileTrack.getDuration()).toEqual(track.getDuration());
        expect(apiMobileTrack.getGenre()).toEqual(track.getGenre());
        expect(apiMobileTrack.getLikesCount()).toEqual(track.getLikesCount());
        expect(apiMobileTrack.getPermalinkUrl()).toEqual(track.getPermalinkUrl());
        expect(apiMobileTrack.getPlaybackCount()).toEqual(track.getPlaybackCount());
        expect(apiMobileTrack.getPolicy()).toEqual(track.getPolicy());
        expect(apiMobileTrack.getRepostsCount()).toEqual(track.getRepostsCount());
        expect(apiMobileTrack.getSharing()).toEqual(track.getSharing());
        expect(apiMobileTrack.getStreamUrl()).toEqual(track.getStreamUrl());
        expect(apiMobileTrack.getUserTags()).toEqual(track.humanTags());
        expect(apiMobileTrack.getTitle()).toEqual(track.getTitle());
        expect(apiMobileTrack.getUserName()).toEqual(track.getUserName());
        expect(apiMobileTrack.getWaveformUrl()).toEqual(track.getWaveformUrl());
        expect(apiMobileTrack.getUrn()).toEqual(track.getUrn());

        PublicApiUserTest.assertApiUsersEqual((ApiUser) apiMobileTrack.getUser(), track.getUser());

    }

    @Test
    public void shouldConvertToPropertySetWithBlankUsernameIfUsernameNull() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        track.setUser(new PublicApiUser(1L));

        PropertySet propertySet = track.toPropertySet();
        expect(propertySet.get(PlayableProperty.CREATOR_NAME)).toEqual(ScTextUtils.EMPTY_STRING);
    }

    @Test
    public void shouldConvertToPropertySetWithUserUrnCreatedFromUserId() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        track.setUser(null);

        PropertySet propertySet = track.toPropertySet();
        expect(propertySet.get(PlayableProperty.CREATOR_URN)).toEqual(Urn.forUser(track.getUserId()));
    }

    private void compareTracks(PublicApiTrack t, PublicApiTrack t2) {
        expect(t2.getId()).toEqual(t.getId());
        expect(t2.title).toEqual(t.title);
        expect(t2.permalink).toEqual(t.permalink);
        expect(t2.policy).toEqual(t.policy);
        expect(t2.isMonetizable()).toEqual(t.isMonetizable());
        expect(t2.duration).toBeGreaterThan(0L);
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
