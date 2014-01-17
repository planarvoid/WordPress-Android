package com.soundcloud.android.events;

import static org.junit.Assert.assertEquals;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class UIEventTest {

    @Test
    public void shouldCreateEventFromFollow() throws Exception {
        UIEvent uiEvent = UIEvent.fromFollow("screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 0);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("user_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnfollow() throws Exception {
        UIEvent uiEvent = UIEvent.fromUnfollow("screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 1);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("user_id"), "30");
    }

    @Test
    public void shouldCreateEventFromToggleToFollow() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleFollow(true, "screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 0);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("user_id"), "30");
    }

    @Test
    public void shouldCreateEventFromToggleToUnfollow() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleFollow(false, "screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 1);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("user_id"), "30");
    }

    @Test
    public void shouldCreateEventFromLikedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromLike("screen", new Track(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 2);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromLike("screen", new Playlist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 2);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromUnlike("screen", new Track(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 3);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromUnlike("screen", new Playlist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 3);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromRepost("screen", new Track(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 4);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromRepost("screen", new Playlist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 4);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromUnrepost("screen", new Track(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 5);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromUnrepost("screen", new Playlist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 5);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistIsNew() throws Exception {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("screen", true, 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 6);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("is_new_playlist"), "yes");
        assertEquals(uiEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistExisted() throws Exception {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("screen", false, 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 6);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("is_new_playlist"), "no");
        assertEquals(uiEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldCreateEventFromComment() throws Exception {
        UIEvent uiEvent = UIEvent.fromComment("screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 7);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldCreateEventFromTrackShare() throws Exception {
        UIEvent uiEvent = UIEvent.fromShare("screen", new Track(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 8);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromPlaylistShare() throws Exception {
        UIEvent uiEvent = UIEvent.fromShare("screen", new Playlist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 8);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromShuffleMyLikes() throws Exception {
        assertEquals(UIEvent.fromShuffleMyLikes().getKind(), 9);
    }
}
