package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.profile.UserSoundsTypes;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ModuleTest {
    @Test
    public void shouldConvertAlbumsToModule() {
        final String resource = "whatevs";
        final Optional<Module> module = Module.getModuleFromUserSoundsType(UserSoundsTypes.ALBUMS, resource);
        assertThat(module.get().getName()).isEqualTo(Module.USER_ALBUMS);
        assertThat(module.get().getResource()).isEqualTo(resource);
    }

    @Test
    public void shouldConvertPlaylistsToModule() {
        final String resource = "whatevs";
        final Optional<Module> module = Module.getModuleFromUserSoundsType(UserSoundsTypes.PLAYLISTS, resource);
        assertThat(module.get().getName()).isEqualTo(Module.USER_PLAYLISTS);
        assertThat(module.get().getResource()).isEqualTo(resource);
    }

    @Test
    public void shouldConvertLikesToModule() {
        final String resource = "whatevs";
        final Optional<Module> module = Module.getModuleFromUserSoundsType(UserSoundsTypes.LIKES, resource);
        assertThat(module.get().getName()).isEqualTo(Module.USER_LIKES);
        assertThat(module.get().getResource()).isEqualTo(resource);
    }

    @Test
    public void shouldConvertRepostsToModule() {
        final String resource = "whatevs";
        final Optional<Module> module = Module.getModuleFromUserSoundsType(UserSoundsTypes.REPOSTS, resource);
        assertThat(module.get().getName()).isEqualTo(Module.USER_REPOSTS);
        assertThat(module.get().getResource()).isEqualTo(resource);
    }

    @Test
    public void shouldConvertSpotlightToModule() {
        final String resource = "whatevs";
        final Optional<Module> module = Module.getModuleFromUserSoundsType(UserSoundsTypes.SPOTLIGHT, resource);
        assertThat(module.get().getName()).isEqualTo(Module.USER_SPOTLIGHT);
        assertThat(module.get().getResource()).isEqualTo(resource);
    }

    @Test
    public void shouldReturnAbsentWhenUnexpectedType() {
        final String resource = "whatevs";
        final Optional<Module> module = Module.getModuleFromUserSoundsType(-1, resource);
        assertThat(module.isPresent()).isFalse();
    }
}
