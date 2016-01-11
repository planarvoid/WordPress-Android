package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

import java.util.Collections;

public class SearchItemTest extends AndroidUnitTest {

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
        final PropertySet propertySet = PropertySet.create();
        propertySet.put(EntityProperty.URN, Urn.forTrack(123L));
        propertySet.put(TrackProperty.DESCRIPTION, "IronMan");

        final ListItem listItem = SearchItem.fromPropertySet(propertySet).build();

        assertThat(listItem).isInstanceOf(TrackItem.class);
    }

    @Test
    public void shouldBuildCorrectPlaylistListItemType() {
        final PropertySet propertySet = PropertySet.create();
        propertySet.put(EntityProperty.URN, Urn.forPlaylist(123L));
        propertySet.put(PlaylistProperty.IS_POSTED, true);

        final ListItem listItem = SearchItem.fromPropertySet(propertySet).build();

        assertThat(listItem).isInstanceOf(PlaylistItem.class);
    }

    @Test
    public void shouldBuildCorrectUserListItemType() {
        final PropertySet propertySet = PropertySet.create();
        propertySet.put(EntityProperty.URN, Urn.forUser(123L));
        propertySet.put(UserProperty.COUNTRY, "Argentina");

        final ListItem listItem = SearchItem.fromPropertySet(propertySet).build();

        assertThat(listItem).isInstanceOf(UserItem.class);
    }

    @Test
    public void shouldBuildPremiumItem() {
        ListItem listItem = SearchItem.buildPremiumItem(Collections.<PropertySet>emptyList());

        assertThat(listItem).isInstanceOf(SearchPremiumItem.class);
    }
}
