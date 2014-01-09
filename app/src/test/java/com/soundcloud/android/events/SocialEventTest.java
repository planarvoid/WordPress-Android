package com.soundcloud.android.events;

import static org.junit.Assert.assertEquals;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class SocialEventTest {
    @Test
    public void shouldCreateEventFromFollow() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromFollow("screen", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 0);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.userId, 30L);
    }

    @Test
    public void shouldCreateEventFromUnfollow() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnfollow("screen", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 1);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.userId, 30L);
    }

    @Test
    public void shouldCreateEventFromLikedTrack() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromLike("screen", new Track(30));
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 2);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "track");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromLike("screen", new Playlist(30));
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 2);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "playlist");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnlike("screen", new Track(30));
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 3);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "track");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnlike("screen", new Playlist(30));
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 3);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "playlist");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromRepost("screen", new Track(30));
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 4);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "track");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromRepost("screen", new Playlist(30));
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 4);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "playlist");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnrepost("screen", new Track(30));
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 5);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "track");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnrepost("screen", new Playlist(30));
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 5);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "playlist");
        assertEquals(socialEventAttributes.resourceId, 30);
    }

    @Test
    public void shouldCreateEventFromAddToPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromAddToPlaylist("screen", true, 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 6);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.isNewPlaylist, true);
        assertEquals(socialEventAttributes.trackId, 30);
    }

    @Test
    public void shouldCreateEventFromComment() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromComment("screen", 30);
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 7);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.trackId, 30);
    }

    @Test
    public void shouldCreateEventFromTrackShare() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromShare("screen", new Track(30), "facebook");
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 8);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "track");
        assertEquals(socialEventAttributes.resourceId, 30);
        assertEquals(socialEventAttributes.sharedTo, "facebook");
    }

    @Test
    public void shouldCreateEventFromPlaylistShare() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromShare("screen", new Playlist(30), "facebook");
        SocialEvent.Attributes socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getType(), 8);
        assertEquals(socialEventAttributes.screenTag, "screen");
        assertEquals(socialEventAttributes.resource, "playlist");
        assertEquals(socialEventAttributes.resourceId, 30);
        assertEquals(socialEventAttributes.sharedTo, "facebook");
    }
}
