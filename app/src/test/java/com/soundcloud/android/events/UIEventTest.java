package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;
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
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "screen", new Track(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 2);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "screen", new Playlist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 2);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "screen", new Track(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 3);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "screen", new Playlist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 3);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", new Track(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 4);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", new Playlist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 4);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", new Track(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), 5);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", new Playlist(30));
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
        expect(UIEvent.fromShuffleMyLikes().getKind()).toEqual(9);
    }

    @Test
    public void shouldCreateEventFromProfileNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromProfileNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(10);
        expect(uiEventAttributes.get("page")).toEqual("you");
    }

    @Test
    public void shouldCreateEventFromStreamNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromStreamNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(10);
        expect(uiEventAttributes.get("page")).toEqual("stream");
    }

    @Test
    public void shouldCreateEventFromExploreNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromExploreNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(10);
        expect(uiEventAttributes.get("page")).toEqual("explore");
    }

    @Test
    public void shouldCreateEventFromLikesNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromLikesNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(10);
        expect(uiEventAttributes.get("page")).toEqual("collection_likes");
    }

    @Test
    public void shouldCreateEventFromPlaylistsNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromPlaylistsNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(10);
        expect(uiEventAttributes.get("page")).toEqual("collection_playlists");
    }

    @Test
    public void shouldCreateEventFromSearchNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromSearchAction();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(10);
        expect(uiEventAttributes.get("page")).toEqual("search");
    }

    @Test
    public void shouldCreateEventFromPlayerShortcutNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromPlayerShortcut();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(10);
        expect(uiEventAttributes.get("page")).toEqual("player_shortcut");
    }

    @Test
    public void shouldCreateEventFromPlayerExpanded() {
        UIEvent uiEvent = UIEvent.fromPlayerExpanded();
        expect(uiEvent.getKind()).toEqual(11);
    }

    @Test
    public void shouldCreateEventFromPlayerCollapsed() {
        UIEvent uiEvent = UIEvent.fromPlayerCollapsed();
        expect(uiEvent.getKind()).toEqual(12);
    }

}
