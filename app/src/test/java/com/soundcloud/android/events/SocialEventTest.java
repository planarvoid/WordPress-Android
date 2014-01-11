package com.soundcloud.android.events;

import static org.junit.Assert.assertEquals;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class SocialEventTest {
    @Test
    public void shouldCreateEventFromFollow() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromFollow("screen", 30);
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 0);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("user_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnfollow() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnfollow("screen", 30);
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 1);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("user_id"), "30");
    }

    @Test
    public void shouldCreateEventFromLikedTrack() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromLike("screen", new Track(30));
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 2);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("resource"), "track");
        assertEquals(socialEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromLike("screen", new Playlist(30));
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 2);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("resource"), "playlist");
        assertEquals(socialEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnlike("screen", new Track(30));
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 3);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("resource"), "track");
        assertEquals(socialEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnlike("screen", new Playlist(30));
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 3);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("resource"), "playlist");
        assertEquals(socialEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromRepost("screen", new Track(30));
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 4);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("resource"), "track");
        assertEquals(socialEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromRepost("screen", new Playlist(30));
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 4);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("resource"), "playlist");
        assertEquals(socialEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnrepost("screen", new Track(30));
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 5);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("resource"), "track");
        assertEquals(socialEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromUnrepost("screen", new Playlist(30));
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 5);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("resource"), "playlist");
        assertEquals(socialEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistIsNew() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromAddToPlaylist("screen", true, 30);
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 6);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("is_new_playlist"), "yes");
        assertEquals(socialEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistExisted() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromAddToPlaylist("screen", false, 30);
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 6);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("is_new_playlist"), "no");
        assertEquals(socialEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldCreateEventFromComment() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromComment("screen", 30);
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 7);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldCreateEventFromTrackShare() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromShare("screen", new Track(30));
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 8);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("resource"), "track");
        assertEquals(socialEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromPlaylistShare() throws Exception {
        SocialEvent socialEvent = SocialEvent.fromShare("screen", new Playlist(30));
        Map<String, String> socialEventAttributes = socialEvent.getAttributes();
        assertEquals(socialEvent.getKind(), 8);
        assertEquals(socialEventAttributes.get("context"), "screen");
        assertEquals(socialEventAttributes.get("resource"), "playlist");
        assertEquals(socialEventAttributes.get("resource_id"), "30");
    }
}
