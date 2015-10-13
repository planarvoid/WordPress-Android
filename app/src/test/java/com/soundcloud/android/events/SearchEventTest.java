package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import org.junit.Before;
import org.junit.Test;

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
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUGGESTION);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("track");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("global");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("item_navigation");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromGlobalSuggestionUserSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.USER, false, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUGGESTION);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("user");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("global");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("item_navigation");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromLocalSuggestionTrackSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.TRACK, true, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUGGESTION);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("track");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("personal");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("item_navigation");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromLocalSuggestionUserSearch() {
        searchEvent = SearchEvent.searchSuggestion(Content.USER, true, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUGGESTION);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("user");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("personal");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("item_navigation");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }


    @Test
    public void shouldCreateEventFromRecentTagSearch() {
        searchEvent = SearchEvent.recentTagSearch("indie rock");
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUBMIT);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("tag");
        assertThat(searchEvent.getAttributes().get("location")).isEqualTo("recent_tags");
        assertThat(searchEvent.getAttributes().get("content")).isEqualTo("indie rock");
    }

    @Test
    public void shouldCreateEventFromPopularTagSearch() {
        searchEvent = SearchEvent.popularTagSearch("indie rock");
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUBMIT);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("tag");
        assertThat(searchEvent.getAttributes().get("location")).isEqualTo("popular_tags");
        assertThat(searchEvent.getAttributes().get("content")).isEqualTo("indie rock");
    }

    @Test
    public void shouldCreateEventFromNormalSearchViaField() {
        searchEvent = SearchEvent.searchField("a query", false, false);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUBMIT);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("normal");
        assertThat(searchEvent.getAttributes().get("location")).isEqualTo("search_field");
        assertThat(searchEvent.getAttributes().get("content")).isEqualTo("a query");
    }

    @Test
    public void shouldCreateEventFromNormalSearchViaSuggestion() {
        searchEvent = SearchEvent.searchField("a query", true, false);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUBMIT);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("normal");
        assertThat(searchEvent.getAttributes().get("location")).isEqualTo("search_suggestion");
        assertThat(searchEvent.getAttributes().get("content")).isEqualTo("a query");
    }

    @Test
    public void shouldCreateEventFromTagSearchViaField() {
        searchEvent = SearchEvent.searchField("indie rock", false, true);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUBMIT);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("tag");
        assertThat(searchEvent.getAttributes().get("location")).isEqualTo("search_field");
        assertThat(searchEvent.getAttributes().get("content")).isEqualTo("indie rock");
    }

    @Test
    public void shouldCreateEventFromTagSearchViaSuggestion() {
        searchEvent = SearchEvent.searchField("indie rock", true, true);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUBMIT);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("tag");
        assertThat(searchEvent.getAttributes().get("location")).isEqualTo("search_suggestion");
        assertThat(searchEvent.getAttributes().get("content")).isEqualTo("indie rock");
    }

    @Test
    public void shouldCreateEventFromTapTrackOnEverythingTab() {
        searchEvent = SearchEvent.tapTrackOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_RESULTS);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("track");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("everything");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("play");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnEverythingTab() {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_RESULTS);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("playlist");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("everything");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("open_playlist");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapUserOnEverythingTab() {
        searchEvent = SearchEvent.tapUserOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_RESULTS);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("user");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("everything");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("open_profile");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapTrackOnTracksTab() {
        searchEvent = SearchEvent.tapTrackOnScreen(Screen.SEARCH_TRACKS, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_RESULTS);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("track");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("tracks");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("play");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnPlaylistsTab() {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLISTS, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_RESULTS);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("playlist");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("playlists");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("open_playlist");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapUserOnPeopleTab() {
        searchEvent = SearchEvent.tapUserOnScreen(Screen.SEARCH_USERS, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_RESULTS);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("user");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("people");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("open_profile");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnPlaylistTagResults() throws Exception {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLIST_DISCO, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_RESULTS);
        assertThat(searchEvent.getAttributes().get("type")).isEqualTo("playlist");
        assertThat(searchEvent.getAttributes().get("context")).isEqualTo("tags");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(searchEvent.getClickPosition()).isEqualTo(1);
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("open_playlist");
        assertThat(searchEvent.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:456");
    }

    @Test
    public void shouldCreateEventFromSearchStart() throws Exception {
        searchEvent = SearchEvent.searchStart(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEvent.getKind()).isSameAs(SearchEvent.KIND_SUBMIT);
        assertThat(searchEvent.getAttributes().get("page_name")).isEqualTo("search:everything");
        assertThat(searchEvent.getAttributes().get("click_name")).isEqualTo("search");
        assertThat(searchEvent.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
    }
}
