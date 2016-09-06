package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.Module;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UserSoundsTypesTest {
    @Test
    public void shouldConvertAlbumsToModule() {
        final int position = 1;
        final Module module = UserSoundsTypes.fromModule(UserSoundsTypes.ALBUMS, position);
        assertThat(module.getName()).isEqualTo(Module.USER_ALBUMS);
        assertThat(module.getPosition()).isEqualTo(position);
    }

    @Test
    public void shouldConvertPlaylistsToModule() {
        final int position = 1;
        final Module module = UserSoundsTypes.fromModule(UserSoundsTypes.PLAYLISTS, position);
        assertThat(module.getName()).isEqualTo(Module.USER_PLAYLISTS);
        assertThat(module.getPosition()).isEqualTo(position);
    }

    @Test
    public void shouldConvertLikesToModule() {
        final int position = 1;
        final Module module = UserSoundsTypes.fromModule(UserSoundsTypes.LIKES, position);
        assertThat(module.getName()).isEqualTo(Module.USER_LIKES);
        assertThat(module.getPosition()).isEqualTo(position);
    }

    @Test
    public void shouldConvertRepostsToModule() {
        final int position = 1;
        final Module module = UserSoundsTypes.fromModule(UserSoundsTypes.REPOSTS, position);
        assertThat(module.getName()).isEqualTo(Module.USER_REPOSTS);
        assertThat(module.getPosition()).isEqualTo(position);
    }

    @Test
    public void shouldConvertSpotlightToModule() {
        final int position = 1;
        final Module module = UserSoundsTypes.fromModule(UserSoundsTypes.SPOTLIGHT, position);
        assertThat(module.getName()).isEqualTo(Module.USER_SPOTLIGHT);
        assertThat(module.getPosition()).isEqualTo(position);
    }

    @Test
    public void shouldConvertTracksToModule() {
        final int position = 1;
        final Module module = UserSoundsTypes.fromModule(UserSoundsTypes.TRACKS, position);
        assertThat(module.getName()).isEqualTo(Module.USER_TRACKS);
        assertThat(module.getPosition()).isEqualTo(position);
    }
}
