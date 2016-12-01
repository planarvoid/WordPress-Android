package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.NewTrackingEvent.Kind;
import com.soundcloud.android.events.SearchEvent.ClickName;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;

public class SearchEventTest {

    private final Urn QUERY_URN = new Urn("soundcloud:search:123");
    private final Urn CLICK_OBJECT_URN = new Urn("soundcloud:tracks:456");
    private SearchEvent searchEvent;
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Before
    public void setup() throws Exception {
        searchQuerySourceInfo = new SearchQuerySourceInfo(QUERY_URN, 1, CLICK_OBJECT_URN, "query");
    }

    @Test
    public void shouldCreateEventFromRecentTagSearch() {
        searchEvent =  SearchEvent.recentTagSearch();
        assertThat(searchEvent.kind()).isSameAs(Kind.SEARCH_SUBMIT);
        assertThat(searchEvent.clickName().get()).isEqualTo(ClickName.ITEM_NAVIGATION);
    }

    @Test
    public void shouldCreateEventFromPopularTagSearch() {
        searchEvent = SearchEvent.popularTagSearch();
        assertThat(searchEvent.kind()).isSameAs(Kind.SEARCH_SUBMIT);
    }

    @Test
    public void shouldCreateEventFromTapTrackOnEverythingTab() {
        searchEvent = SearchEvent.tapTrackOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEvent.kind()).isSameAs(Kind.SEARCH_RESULTS);
        assertThat(searchEvent.queryUrn().get()).isEqualTo(QUERY_URN);
        assertThat(searchEvent.queryPosition().get()).isEqualTo(1);
        assertThat(searchEvent.clickName().get()).isEqualTo(ClickName.ITEM_NAVIGATION);
        assertThat(searchEvent.clickObject().get()).isEqualTo(CLICK_OBJECT_URN);
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnEverythingTab() {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEvent.kind()).isSameAs(Kind.SEARCH_RESULTS);
        assertThat(searchEvent.queryUrn().get()).isEqualTo(QUERY_URN);
        assertThat(searchEvent.queryPosition().get()).isEqualTo(1);
        assertThat(searchEvent.clickName().get()).isEqualTo(ClickName.ITEM_NAVIGATION);
        assertThat(searchEvent.clickObject().get()).isEqualTo(CLICK_OBJECT_URN);
    }

    @Test
    public void shouldCreateEventFromTapUserOnEverythingTab() {
        searchEvent = SearchEvent.tapUserOnScreen(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEvent.kind()).isSameAs(Kind.SEARCH_RESULTS);
        assertThat(searchEvent.queryUrn().get()).isEqualTo(QUERY_URN);
        assertThat(searchEvent.queryPosition().get()).isEqualTo(1);
        assertThat(searchEvent.clickName().get()).isEqualTo(ClickName.ITEM_NAVIGATION);
        assertThat(searchEvent.clickObject().get()).isEqualTo(CLICK_OBJECT_URN);
    }

    @Test
    public void shouldCreateEventFromTapTrackOnTracksTab() {
        searchEvent = SearchEvent.tapTrackOnScreen(Screen.SEARCH_TRACKS, searchQuerySourceInfo);
        assertThat(searchEvent.kind()).isSameAs(Kind.SEARCH_RESULTS);
        assertThat(searchEvent.queryUrn().get()).isEqualTo(QUERY_URN);
        assertThat(searchEvent.queryPosition().get()).isEqualTo(1);
        assertThat(searchEvent.clickName().get()).isEqualTo(ClickName.ITEM_NAVIGATION);
        assertThat(searchEvent.clickObject().get()).isEqualTo(CLICK_OBJECT_URN);
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnPlaylistsTab() {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLISTS, searchQuerySourceInfo);
        assertThat(searchEvent.kind()).isSameAs(Kind.SEARCH_RESULTS);
        assertThat(searchEvent.queryUrn().get()).isEqualTo(QUERY_URN);
        assertThat(searchEvent.queryPosition().get()).isEqualTo(1);
        assertThat(searchEvent.clickName().get()).isEqualTo(ClickName.ITEM_NAVIGATION);
        assertThat(searchEvent.clickObject().get()).isEqualTo(CLICK_OBJECT_URN);
    }

    @Test
    public void shouldCreateEventFromTapUserOnPeopleTab() {
        searchEvent = SearchEvent.tapUserOnScreen(Screen.SEARCH_USERS, searchQuerySourceInfo);
        assertThat(searchEvent.kind()).isSameAs(Kind.SEARCH_RESULTS);
        assertThat(searchEvent.queryUrn().get()).isEqualTo(QUERY_URN);
        assertThat(searchEvent.queryPosition().get()).isEqualTo(1);
        assertThat(searchEvent.clickName().get()).isEqualTo(ClickName.ITEM_NAVIGATION);
        assertThat(searchEvent.clickObject().get()).isEqualTo(CLICK_OBJECT_URN);
    }

    @Test
    public void shouldCreateEventFromTapPlaylistOnPlaylistTagResults() throws Exception {
        searchEvent = SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLIST_DISCO, searchQuerySourceInfo);
        assertThat(searchEvent.kind()).isSameAs(Kind.SEARCH_RESULTS);
        assertThat(searchEvent.queryUrn().get()).isEqualTo(QUERY_URN);
        assertThat(searchEvent.queryPosition().get()).isEqualTo(1);
        assertThat(searchEvent.clickName().get()).isEqualTo(ClickName.ITEM_NAVIGATION);
        assertThat(searchEvent.clickObject().get()).isEqualTo(CLICK_OBJECT_URN);
    }

    @Test
    public void shouldCreateEventFromSearchStart() throws Exception {
        searchEvent = SearchEvent.searchStart(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo);
        assertThat(searchEvent.kind()).isSameAs(Kind.SEARCH_SUBMIT);
        assertThat(searchEvent.pageName().get()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
        assertThat(searchEvent.clickName().get()).isEqualTo(ClickName.SEARCH);
        assertThat(searchEvent.queryUrn().get()).isEqualTo(QUERY_URN);
    }
}
