package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.java.strings.Strings;
import org.junit.Test;

public class PlaylistUtilsTest {
    @Test
    public void shouldProvideReleaseYearWhenReleaseDateIsAvailable() {
        assertThat(PlaylistUtils.releaseYear("2010-10-10")).isEqualTo("2010");
    }

    @Test
    public void shouldNotProvideReleaseYearWhenReleaseDateIsNotAvailable() {
        assertThat(PlaylistUtils.releaseYear(Strings.EMPTY)).isEqualTo(Strings.EMPTY);
    }

    @Test
    public void shouldNotProvideReleaseYearWhenReleaseDateIsInvalid() {
        assertThat(PlaylistUtils.releaseYear("invalid")).isEqualTo(Strings.EMPTY);
    }

}
