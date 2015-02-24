package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class SearchEventTest {

    private SearchEvent searchEvent;

    @Test
    public void shouldCreateEventFromGlobalSuggestionTrackSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.TRACK, false);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUGGESTION);
        expect(searchEvent.getAttributes().get("type")).toEqual("track");
        expect(searchEvent.getAttributes().get("context")).toEqual("global");
    }

    @Test
    public void shouldCreateEventFromGlobalSuggestionUserSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.USER, false);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUGGESTION);
        expect(searchEvent.getAttributes().get("type")).toEqual("user");
        expect(searchEvent.getAttributes().get("context")).toEqual("global");
    }

    @Test
    public void shouldCreateEventFromLocalSuggestionTrackSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.TRACK, true);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUGGESTION);
        expect(searchEvent.getAttributes().get("type")).toEqual("track");
        expect(searchEvent.getAttributes().get("context")).toEqual("personal");
    }

    @Test
    public void shouldCreateEventFromLocalSuggestionUserSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.USER, true);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUGGESTION);
        expect(searchEvent.getAttributes().get("type")).toEqual("user");
        expect(searchEvent.getAttributes().get("context")).toEqual("personal");
    }


    @Test
    public void shouldCreateEventFromRecentTagSearch() {
        searchEvent = SearchEvent.recentTagSearch("indie rock");
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(searchEvent.getAttributes().get("type")).toEqual("tag");
        expect(searchEvent.getAttributes().get("location")).toEqual("recent_tags");
        expect(searchEvent.getAttributes().get("content")).toEqual("indie rock");
    }

    @Test
    public void shouldCreateEventFromPopularTagSearch() {
        searchEvent = SearchEvent.popularTagSearch("indie rock");
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(searchEvent.getAttributes().get("type")).toEqual("tag");
        expect(searchEvent.getAttributes().get("location")).toEqual("popular_tags");
        expect(searchEvent.getAttributes().get("content")).toEqual("indie rock");
    }

    @Test
    public void shouldCreateEventFromNormalSearchViaField() {
        searchEvent = SearchEvent.searchField("a query", false, false);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(searchEvent.getAttributes().get("type")).toEqual("normal");
        expect(searchEvent.getAttributes().get("location")).toEqual("search_field");
        expect(searchEvent.getAttributes().get("content")).toEqual("a query");
    }

    @Test
    public void shouldCreateEventFromNormalSearchViaSuggestion() {
        searchEvent = SearchEvent.searchField("a query", true, false);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(searchEvent.getAttributes().get("type")).toEqual("normal");
        expect(searchEvent.getAttributes().get("location")).toEqual("search_suggestion");
        expect(searchEvent.getAttributes().get("content")).toEqual("a query");
    }

    @Test
    public void shouldCreateEventFromTagSearchViaField() {
        searchEvent = SearchEvent.searchField("indie rock", false, true);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(searchEvent.getAttributes().get("type")).toEqual("tag");
        expect(searchEvent.getAttributes().get("location")).toEqual("search_field");
        expect(searchEvent.getAttributes().get("content")).toEqual("indie rock");
    }

    @Test
    public void shouldCreateEventFromTagSearchViaSuggestion() {
        searchEvent = SearchEvent.searchField("indie rock", true, true);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(searchEvent.getAttributes().get("type")).toEqual("tag");
        expect(searchEvent.getAttributes().get("location")).toEqual("search_suggestion");
        expect(searchEvent.getAttributes().get("content")).toEqual("indie rock");
    }

    @Test
    public void shouldCreateEventFromTapTrackOnEverythingTab() {
        searchEvent = SearchEvent.tapTrackOnScreen(Screen.SEARCH_EVERYTHING);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("track");
        expect(searchEvent.getAttributes().get("context")).toEqual("everything");
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnEverythingTab() {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_EVERYTHING);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("playlist");
        expect(searchEvent.getAttributes().get("context")).toEqual("everything");
    }

    @Test
    public void shouldCreateEventFromTapUserOnEverythingTab() {
        searchEvent = SearchEvent.tapUserOnScreen(Screen.SEARCH_EVERYTHING);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("user");
        expect(searchEvent.getAttributes().get("context")).toEqual("everything");
    }

    @Test
    public void shouldCreateEventFromTapTrackOnTracksTab() {
        searchEvent = SearchEvent.tapTrackOnScreen(Screen.SEARCH_TRACKS);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("track");
        expect(searchEvent.getAttributes().get("context")).toEqual("tracks");
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnPlaylistsTab() {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLISTS);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("playlist");
        expect(searchEvent.getAttributes().get("context")).toEqual("playlists");
    }

    @Test
    public void shouldCreateEventFromTapUserOnPeopleTab() {
        searchEvent = SearchEvent.tapUserOnScreen(Screen.SEARCH_USERS);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("user");
        expect(searchEvent.getAttributes().get("context")).toEqual("people");
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnPlaylistTagResults() throws Exception {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLIST_DISCO);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("playlist");
        expect(searchEvent.getAttributes().get("context")).toEqual("tags");
    }
}
