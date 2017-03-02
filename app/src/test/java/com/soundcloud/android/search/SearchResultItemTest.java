package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

public class SearchResultItemTest extends AndroidUnitTest {

    private SearchResultItem searchResultItem;

    @Test
    public void shouldAssertCorrectSearchItemTrackType() {
        final Urn trackUrn = Urn.forTrack(123L);
        searchResultItem = SearchResultItem.fromUrn(trackUrn);

        assertThat(searchResultItem.isTrack()).isTrue();
        assertThat(searchResultItem.isPlaylist()).isFalse();
        assertThat(searchResultItem.isUser()).isFalse();
        assertThat(searchResultItem.isUpsell()).isFalse();
        assertThat(searchResultItem.isPremiumContent()).isFalse();
    }

    @Test
    public void shouldAssertCorrectSearchItemPlaylistType() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        searchResultItem = SearchResultItem.fromUrn(playlistUrn);

        assertThat(searchResultItem.isPlaylist()).isTrue();
        assertThat(searchResultItem.isTrack()).isFalse();
        assertThat(searchResultItem.isUser()).isFalse();
        assertThat(searchResultItem.isUpsell()).isFalse();
        assertThat(searchResultItem.isPremiumContent()).isFalse();
    }

    @Test
    public void shouldAssertCorrectSearchItemUserType() {
        final Urn userUrn = Urn.forUser(123L);
        searchResultItem = SearchResultItem.fromUrn(userUrn);

        assertThat(searchResultItem.isUser()).isTrue();
        assertThat(searchResultItem.isTrack()).isFalse();
        assertThat(searchResultItem.isPlaylist()).isFalse();
        assertThat(searchResultItem.isUpsell()).isFalse();
        assertThat(searchResultItem.isPremiumContent()).isFalse();
    }

    @Test
    public void shouldCheckPremiumContentHasCorrectUrn() {
        searchResultItem = SearchResultItem.fromUrn(SearchPremiumItem.PREMIUM_URN);

        assertThat(searchResultItem.isPremiumContent()).isTrue();
        assertThat(searchResultItem.isUser()).isFalse();
        assertThat(searchResultItem.isTrack()).isFalse();
        assertThat(searchResultItem.isPlaylist()).isFalse();
        assertThat(searchResultItem.isUpsell()).isFalse();
    }

    @Test
    public void shouldCheckSearchUpsellHasCorrectUrn() {
        searchResultItem = SearchResultItem.fromUrn(UpsellSearchableItem.UPSELL_URN);

        assertThat(searchResultItem.isUpsell()).isTrue();
        assertThat(searchResultItem.isUser()).isFalse();
        assertThat(searchResultItem.isTrack()).isFalse();
        assertThat(searchResultItem.isPlaylist()).isFalse();
        assertThat(searchResultItem.isPremiumContent()).isFalse();
    }
}
