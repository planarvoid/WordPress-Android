package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
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
        searchResultItem = SearchResultItem.fromUrn(SearchUpsellItem.UPSELL_URN);

        assertThat(searchResultItem.isUpsell()).isTrue();
        assertThat(searchResultItem.isUser()).isFalse();
        assertThat(searchResultItem.isTrack()).isFalse();
        assertThat(searchResultItem.isPlaylist()).isFalse();
        assertThat(searchResultItem.isPremiumContent()).isFalse();
    }

    @Test
    public void shouldBuildCorrectTrackListItemType() {
        final PropertySet propertySet = PropertySet.create();
        propertySet.put(EntityProperty.URN, Urn.forTrack(123L));
        propertySet.put(TrackProperty.DESCRIPTION, "IronMan");

        final ListItem listItem = SearchResultItem.fromPropertySet(propertySet).build();

        assertThat(listItem).isInstanceOf(TrackItem.class);
    }

    @Test
    public void shouldBuildCorrectPlaylistListItemType() {
        final PropertySet propertySet = PropertySet.create();
        propertySet.put(EntityProperty.URN, Urn.forPlaylist(123L));

        final ListItem listItem = SearchResultItem.fromPropertySet(propertySet).build();

        assertThat(listItem).isInstanceOf(PlaylistItem.class);
    }

    @Test
    public void shouldBuildCorrectUserListItemType() {
        final PropertySet propertySet = PropertySet.create();
        propertySet.put(EntityProperty.URN, Urn.forUser(123L));
        propertySet.put(UserProperty.COUNTRY, "Argentina");

        final ListItem listItem = SearchResultItem.fromPropertySet(propertySet).build();

        assertThat(listItem).isInstanceOf(UserItem.class);
    }

    @Test
    public void shouldBuildCorrectSearchUpsellItemType() {
        final PropertySet propertySet = PropertySet.create();
        propertySet.put(EntityProperty.URN, SearchUpsellItem.UPSELL_URN);

        final ListItem listItem = SearchResultItem.fromPropertySet(propertySet).build();

        assertThat(listItem).isInstanceOf(SearchUpsellItem.class);
    }
}
