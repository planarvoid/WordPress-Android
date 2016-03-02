package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchPremiumItem.PREMIUM_URN;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class SearchPremiumItemTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(1L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(2L);
    private static final Urn USER_URN = Urn.forUser(3L);
    private static final int RESULTS_COUNT = 10;

    private SearchPremiumItem searchPremiumItem;

    @Before
    public void setUp() {
        final PropertySet propertySet = PropertySet.create().put(EntityProperty.URN, TRACK_URN);
        searchPremiumItem = buildWithPropertySet(propertySet);
    }

    @Test
    public void shouldHaveCorrectUrn() {
        assertThat(searchPremiumItem.getEntityUrn()).isEqualTo(PREMIUM_URN);
    }

    @Test
    public void shouldBuildTrackAsFirstItem() {
        final PropertySet propertySet = PropertySet.create().put(EntityProperty.URN, TRACK_URN);
        searchPremiumItem = buildWithPropertySet(propertySet);

        assertThat(searchPremiumItem.getFirstItem()).isInstanceOf(TrackItem.class);
        assertThat(searchPremiumItem.getFirstItem().getEntityUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void shouldBuildPlaylistAsFirstItem() {
        final PropertySet propertySet = PropertySet.create().put(EntityProperty.URN, PLAYLIST_URN);
        searchPremiumItem = buildWithPropertySet(propertySet);

        assertThat(searchPremiumItem.getFirstItem()).isInstanceOf(PlaylistItem.class);
        assertThat(searchPremiumItem.getFirstItem().getEntityUrn()).isEqualTo(PLAYLIST_URN);
    }

    @Test
    public void shouldBuildUserAsFirstItem() {
        final PropertySet propertySet = PropertySet.create().put(EntityProperty.URN, USER_URN);
        searchPremiumItem = buildWithPropertySet(propertySet);

        assertThat(searchPremiumItem.getFirstItem()).isInstanceOf(UserItem.class);
        assertThat(searchPremiumItem.getFirstItem().getEntityUrn()).isEqualTo(USER_URN);
    }

    @Test
    public void shouldSetTrackPlayingIfFirstItemIsTrack() {
        final PropertySet propertySet = PropertySet.create().put(EntityProperty.URN, TRACK_URN);
        searchPremiumItem = buildWithPropertySet(propertySet);

        final TrackItem trackItem = (TrackItem) searchPremiumItem.getFirstItem();
        assertThat(trackItem.isPlaying()).isFalse();

        searchPremiumItem.setTrackIsPlaying(TRACK_URN);
        assertThat(trackItem.isPlaying()).isTrue();
    }

    @Test
    public void shouldNotSetTrackPlayingWithIncorrectUrn() {
        final PropertySet propertySet = PropertySet.create().put(EntityProperty.URN, TRACK_URN);
        searchPremiumItem = buildWithPropertySet(propertySet);

        final TrackItem trackItem = (TrackItem) searchPremiumItem.getFirstItem();
        assertThat(trackItem.isPlaying()).isFalse();

        searchPremiumItem.setTrackIsPlaying(USER_URN);
        assertThat(trackItem.isPlaying()).isFalse();
    }

    private SearchPremiumItem buildWithPropertySet(PropertySet propertySet) {
        return new SearchPremiumItem(Collections.singletonList(propertySet), Optional.<Link>absent(), RESULTS_COUNT);
    }
}
