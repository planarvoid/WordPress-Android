package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class SearchEventTest {

    private SearchEvent searchEvent;
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Before
    public void setup() throws Exception {
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 1, Urn.forTrack(456L));
    }

    @Test
    public void shouldCreateEventFromGlobalSuggestionTrackSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.TRACK, false, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUGGESTION);
        expect(searchEvent.getAttributes().get("type")).toEqual("track");
        expect(searchEvent.getAttributes().get("context")).toEqual("global");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("item_navigation");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromGlobalSuggestionUserSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.USER, false, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUGGESTION);
        expect(searchEvent.getAttributes().get("type")).toEqual("user");
        expect(searchEvent.getAttributes().get("context")).toEqual("global");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("item_navigation");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromLocalSuggestionTrackSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.TRACK, true, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUGGESTION);
        expect(searchEvent.getAttributes().get("type")).toEqual("track");
        expect(searchEvent.getAttributes().get("context")).toEqual("personal");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("item_navigation");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromLocalSuggestionUserSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.USER, true, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUGGESTION);
        expect(searchEvent.getAttributes().get("type")).toEqual("user");
        expect(searchEvent.getAttributes().get("context")).toEqual("personal");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("item_navigation");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
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
        searchEvent = SearchEvent.tapTrackOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("track");
        expect(searchEvent.getAttributes().get("context")).toEqual("everything");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("play");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnEverythingTab() {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("playlist");
        expect(searchEvent.getAttributes().get("context")).toEqual("everything");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("open_playlist");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapUserOnEverythingTab() {
        searchEvent = SearchEvent.tapUserOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("user");
        expect(searchEvent.getAttributes().get("context")).toEqual("everything");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("open_profile");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapTrackOnTracksTab() {
        searchEvent = SearchEvent.tapTrackOnScreen(Screen.SEARCH_TRACKS, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("track");
        expect(searchEvent.getAttributes().get("context")).toEqual("tracks");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("play");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnPlaylistsTab() {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLISTS, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("playlist");
        expect(searchEvent.getAttributes().get("context")).toEqual("playlists");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("open_playlist");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapUserOnPeopleTab() {
        searchEvent = SearchEvent.tapUserOnScreen(Screen.SEARCH_USERS, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("user");
        expect(searchEvent.getAttributes().get("context")).toEqual("people");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("open_profile");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnPlaylistTagResults() throws Exception {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLIST_DISCO, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_RESULTS);
        expect(searchEvent.getAttributes().get("type")).toEqual("playlist");
        expect(searchEvent.getAttributes().get("context")).toEqual("tags");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(searchEvent.getAttributes().get("click_position")).toEqual("1");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("open_playlist");
        expect(searchEvent.getAttributes().get("click_object")).toEqual("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromSearchStart() throws Exception {
        searchEvent = SearchEvent.searchStart(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        expect(searchEvent.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(searchEvent.getAttributes().get("page_name")).toEqual("search:everything");
        expect(searchEvent.getAttributes().get("click_name")).toEqual("search");
        expect(searchEvent.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
    }
}
