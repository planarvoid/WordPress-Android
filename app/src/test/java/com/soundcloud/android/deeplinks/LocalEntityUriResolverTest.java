package com.soundcloud.android.deeplinks;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;

public class LocalEntityUriResolverTest {

    private LocalEntityUriResolver resolver;

    @Before
    public void setUp() throws Exception {
        resolver = new LocalEntityUriResolver();
    }

    @Test
    public void localResolvableRegex() throws Exception {
        assertTrue(resolver.canResolveLocally("https://soundcloud.com/system-playlists/123"));
        assertTrue(resolver.canResolveLocally("https://soundcloud.com/system-playlists/soundcloud:system-playlists:123:soundcloud:users:123"));
        assertTrue(resolver.canResolveLocally("http://soundcloud.com/playlists/123"));
        assertTrue(resolver.canResolveLocally("http://soundcloud.com/playlists/soundcloud:playlists:123"));
        assertTrue(resolver.canResolveLocally("http://www.soundcloud.com/tracks/123"));
        assertTrue(resolver.canResolveLocally("http://www.soundcloud.com/tracks/soundcloud:tracks:123"));
        assertTrue(resolver.canResolveLocally("https://www.soundcloud.com/users/123"));
        assertTrue(resolver.canResolveLocally("https://www.soundcloud.com/users/soundcloud:users:123"));
        assertTrue(resolver.canResolveLocally("https://www.m.soundcloud.com/users/123"));
        assertTrue(resolver.canResolveLocally("https://www.m.soundcloud.com/users/soundcloud:users:123"));
        assertTrue(resolver.canResolveLocally("http://www.m.soundcloud.com/users/123"));
        assertTrue(resolver.canResolveLocally("http://www.m.soundcloud.com/users/soundcloud:users:123"));
        assertTrue(resolver.canResolveLocally("https://m.soundcloud.com/users/123"));
        assertTrue(resolver.canResolveLocally("https://m.soundcloud.com/users/soundcloud:users:123"));
        assertTrue(resolver.canResolveLocally("http://m.soundcloud.com/users/123"));
        assertTrue(resolver.canResolveLocally("http://m.soundcloud.com/users/soundcloud:users:123"));
        assertTrue(resolver.canResolveLocally("soundcloud://users/123"));
        assertTrue(resolver.canResolveLocally("soundcloud://users/soundcloud:users:123"));
        assertTrue(resolver.canResolveLocally("soundcloud://users:123"));
        assertTrue(resolver.canResolveLocally("soundcloud://tracks/soundcloud:tracks:123"));
        assertTrue(resolver.canResolveLocally("soundcloud://tracks:123"));
        assertTrue(resolver.canResolveLocally("soundcloud:tracks:123"));
        assertTrue(resolver.canResolveLocally("soundcloud://system-playlists:123"));
        assertTrue(resolver.canResolveLocally("soundcloud:system-playlists:123"));
        assertTrue(resolver.canResolveLocally("soundcloud://system-playlists/soundcloud:system-playlists:123:soundcloud:users:123"));
        assertTrue(resolver.canResolveLocally("soundcloud:system-playlists:123:soundcloud:users:123"));
    }

    @Test
    public void correctlyExtractsUrnsLocally() throws Exception {
        resolver.resolve("https://soundcloud.com/system-playlists/123").test().assertValue(Urn.forSystemPlaylist("123"));
        resolver.resolve("https://soundcloud.com/system-playlists/soundcloud:system-playlists:123:soundcloud:users:123").test().assertValue(Urn.forSystemPlaylist("123:soundcloud:users:123"));
        resolver.resolve("http://soundcloud.com/playlists/123").test().assertValue(Urn.forPlaylist(123L));
        resolver.resolve("http://soundcloud.com/playlists/soundcloud:playlists:123").test().assertValue(Urn.forPlaylist(123L));
        resolver.resolve("http://www.soundcloud.com/tracks/123").test().assertValue(Urn.forTrack(123L));
        resolver.resolve("http://www.soundcloud.com/tracks/soundcloud:tracks:123").test().assertValue(Urn.forTrack(123L));
        resolver.resolve("https://www.soundcloud.com/users/123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("https://www.soundcloud.com/users/soundcloud:users:123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("https://www.m.soundcloud.com/users/123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("https://www.m.soundcloud.com/users/soundcloud:users:123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("http://www.m.soundcloud.com/users/123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("http://www.m.soundcloud.com/users/soundcloud:users:123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("https://m.soundcloud.com/users/123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("https://m.soundcloud.com/users/soundcloud:users:123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("http://m.soundcloud.com/users/123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("http://m.soundcloud.com/users/soundcloud:users:123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("soundcloud://users/123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("soundcloud://users/soundcloud:users:123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("soundcloud://users:123").test().assertValue(Urn.forUser(123L));
        resolver.resolve("soundcloud://tracks/soundcloud:tracks:123").test().assertValue(Urn.forTrack(123L));
        resolver.resolve("soundcloud://tracks:123").test().assertValue(Urn.forTrack(123L));
        resolver.resolve("soundcloud:tracks:123").test().assertValue(Urn.forTrack(123L));
        resolver.resolve("soundcloud://system-playlists:123").test().assertValue(Urn.forSystemPlaylist("123"));
        resolver.resolve("soundcloud:system-playlists:123").test().assertValue(Urn.forSystemPlaylist("123"));
        resolver.resolve("soundcloud://system-playlists/soundcloud:system-playlists:123:soundcloud:users:123").test().assertValue(Urn.forSystemPlaylist("123:soundcloud:users:123"));
        resolver.resolve("soundcloud:system-playlists:123:soundcloud:users:123").test().assertValue(Urn.forSystemPlaylist("123:soundcloud:users:123"));
    }

    @Test
    public void localUnresolvableRegex() throws Exception {
        assertFalse(resolver.canResolveLocally("http://test.com/playlists/123"));
        assertFalse(resolver.canResolveLocally("http://www.soundcloud.com/charts/123"));
        assertFalse(resolver.canResolveLocally("https://www.soundcloud.com/theuploads/123"));
        assertFalse(resolver.canResolveLocally("https://www.w.soundcloud.com/users/123"));
        assertFalse(resolver.canResolveLocally("http://www.m.soundcloud.com/users"));
        assertFalse(resolver.canResolveLocally("https://m.soundcloud.com/"));
        assertFalse(resolver.canResolveLocally("m.soundcloud.com/users/123"));
        assertFalse(resolver.canResolveLocally("testing://users/123"));
        assertFalse(resolver.canResolveLocally("ssoundcloud://users/123"));
        assertFalse(resolver.canResolveLocally("soundcloud://user/123"));
        assertFalse(resolver.canResolveLocally("soundcloud://users:123?test=test"));
    }

}
