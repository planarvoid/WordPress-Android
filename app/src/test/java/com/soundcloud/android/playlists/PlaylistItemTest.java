package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;

public class PlaylistItemTest extends AndroidUnitTest {
    private PropertySet propertySet;

    @Before
    public void setUp() throws Exception {
        propertySet = PropertySet.from(
                PlayableProperty.URN.bind(Urn.forPlaylist(123)),
                PlayableProperty.TITLE.bind("title"),
                PlayableProperty.CREATOR_NAME.bind("creator"),
                PlayableProperty.LIKES_COUNT.bind(5),
                PlayableProperty.IS_USER_LIKE.bind(false),
                PlaylistProperty.TRACK_COUNT.bind(11)
        );
    }

    @Test
    public void shouldIdentifyNonAlbums() {
        propertySet.put(PlaylistProperty.IS_ALBUM, false);
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.isAlbum()).isEqualTo(false);
    }

    @Test
    public void shouldIdentifyAlbums() {
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.isAlbum()).isEqualTo(true);
    }

    @Test
    public void shouldProvideSetTypeLabelWhenPlaylistIsNotAnAlbum() {
        propertySet.put(PlaylistProperty.IS_ALBUM, false);
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getSetTypeLabel()).isEqualTo(R.string.set_type_default_label);
    }

    @Test
    public void shouldProvideSetTypeLabelWhenPlaylistIsAnAlbum() {
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        propertySet.put(PlaylistProperty.SET_TYPE, "ep");
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getSetTypeLabel()).isEqualTo(R.string.set_type_ep_label);
    }

    @Test
    public void shouldFallBackToDefaultLabelForUnknownSetTypes() {
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        propertySet.put(PlaylistProperty.SET_TYPE, "unknown");
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getSetTypeLabel()).isEqualTo(R.string.set_type_album_label);
    }

    @Test
    public void shouldProvideSetTypeLabelForTextWhenPlaylistIsNotAnAlbum() {
        propertySet.put(PlaylistProperty.IS_ALBUM, false);
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getSetTypeLabelForText()).isEqualTo(R.string.set_type_default_label_for_text);
    }

    @Test
    public void shouldProvideSetTypeLabelForTextWhenPlaylistIsAnAlbum() {
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        propertySet.put(PlaylistProperty.SET_TYPE, "ep");
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getSetTypeLabelForText()).isEqualTo(R.string.set_type_ep_label_for_text);
    }

    @Test
    public void shouldFallBackToDefaultLabelForTextForUnknownSetTypes() {
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        propertySet.put(PlaylistProperty.SET_TYPE, "unknown");
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getSetTypeLabelForText()).isEqualTo(R.string.set_type_album_label_for_text);
    }

    @Test
    public void shouldProvideReleaseYearWhenReleaseDateIsAvailable() {
        propertySet.put(PlaylistProperty.RELEASE_DATE, "2010-10-10");
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getReleaseYear()).isEqualTo("2010");
    }

    @Test
    public void shouldNotProvideReleaseYearWhenReleaseDateIsNotAvailable() {
        propertySet.put(PlaylistProperty.RELEASE_DATE, Strings.EMPTY);
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getReleaseYear()).isEqualTo(Strings.EMPTY);
    }

    @Test
    public void shouldNotProvideReleaseYearWhenReleaseDateIsInvalid() {
        propertySet.put(PlaylistProperty.RELEASE_DATE, "invalid");
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getReleaseYear()).isEqualTo(Strings.EMPTY);
    }

    @Test
    public void shouldReturnLabelAsPlaylistWhenPlaylistIsNotAnAlbum() {
        propertySet.put(PlaylistProperty.IS_ALBUM, false);
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getLabel(context())).isEqualTo("Playlist");
    }

    @Test
    public void shouldReturnLabelAsSetTypeWhenReleaseDateIsNotAvailable() {
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        propertySet.put(PlaylistProperty.SET_TYPE, "ep");
        propertySet.put(PlaylistProperty.RELEASE_DATE, "2010-10-10");
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getLabel(context())).isEqualTo("EP · 2010");
    }

    @Test
    public void shouldReturnLabelAsSetTypeAndReleaseYearWhenReleaseDateIsAvailable() {
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        propertySet.put(PlaylistProperty.SET_TYPE, "ep");
        PlaylistItem playlistItem = PlaylistItem.from(propertySet);

        assertThat(playlistItem.getLabel(context())).isEqualTo("EP");
    }
}
