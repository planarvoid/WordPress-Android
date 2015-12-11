package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class SearchItemTest {

    private SearchItem searchItem;

    @Test
    public void shouldAssertCorrectSearchItemTrackType() {
        final Urn trackUrn = Urn.forTrack(123L);
        searchItem = SearchItem.fromUrn(trackUrn);

        assertThat(searchItem.isTrack()).isTrue();
        assertThat(searchItem.isPlaylist()).isFalse();
        assertThat(searchItem.isUser()).isFalse();
        assertThat(searchItem.isPremiumContent()).isFalse();
    }

    @Test
    public void shouldAssertCorrectSearchItemPlaylistType() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        searchItem = SearchItem.fromUrn(playlistUrn);

        assertThat(searchItem.isPlaylist()).isTrue();
        assertThat(searchItem.isTrack()).isFalse();
        assertThat(searchItem.isUser()).isFalse();
        assertThat(searchItem.isPremiumContent()).isFalse();
    }

    @Test
    public void shouldAssertCorrectSearchItemUserType() {
        final Urn userUrn = Urn.forUser(123L);
        searchItem = SearchItem.fromUrn(userUrn);

        assertThat(searchItem.isUser()).isTrue();
        assertThat(searchItem.isTrack()).isFalse();
        assertThat(searchItem.isPlaylist()).isFalse();
        assertThat(searchItem.isPremiumContent()).isFalse();
    }

    @Test
    public void shouldCheckPremiumContentHasNotUrnSet() {
        searchItem = SearchItem.fromUrn(Urn.NOT_SET);

        assertThat(searchItem.isPremiumContent()).isTrue();
        assertThat(searchItem.isUser()).isFalse();
        assertThat(searchItem.isTrack()).isFalse();
        assertThat(searchItem.isPlaylist()).isFalse();
    }

    @Test
    public void shouldBuildCorrectTrackListItemType() {
        PropertySet propertySet = PropertySet.create();
        propertySet.put(TrackProperty.DESCRIPTION, "IronMan");

        final Urn trackUrn = Urn.forTrack(123L);
        searchItem = SearchItem.fromUrn(trackUrn);
        final ListItem listItem = searchItem.build(propertySet);

        assertThat(listItem).isInstanceOf(TrackItem.class);
    }

    @Test
    public void shouldBuildCorrectPlaylistListItemType() {
        PropertySet propertySet = PropertySet.create();
        propertySet.put(PlaylistProperty.IS_POSTED, true);

        final Urn playlistUrn = Urn.forPlaylist(123L);
        searchItem = SearchItem.fromUrn(playlistUrn);
        final ListItem listItem = searchItem.build(propertySet);

        assertThat(listItem).isInstanceOf(PlaylistItem.class);
    }

    @Test
    public void shouldBuildCorrectUserListItemType() {
        PropertySet propertySet = PropertySet.create();
        propertySet.put(UserProperty.COUNTRY, "Argentina");

        final Urn userUrn = Urn.forUser(123L);
        searchItem = SearchItem.fromUrn(userUrn);
        final ListItem listItem = searchItem.build(propertySet);

        assertThat(listItem).isInstanceOf(UserItem.class);
    }

    @Test
    public void shouldBuildPremiumItem() {
        ListItem listItem = SearchItem.buildPremiumItem(Collections.<PropertySet>emptyList());

        assertThat(listItem).isInstanceOf(SearchPremiumItem.class);
    }
}
