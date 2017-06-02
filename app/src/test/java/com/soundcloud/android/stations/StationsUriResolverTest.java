package com.soundcloud.android.stations;


import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.deeplinks.UriResolveException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.net.Uri;

public class StationsUriResolverTest extends AndroidUnitTest {

    private StationsUriResolver resolver;

    @Before
    public void setUp() throws Exception {
        resolver = new StationsUriResolver();
    }

    @Test
    public void testResolve() throws Exception {
        assertResolvesToUrn("https://soundcloud.com/stations/artist/123", Urn.forArtistStation(123L));
        assertResolvesToUrn("soundcloud://stations/artist/123", Urn.forArtistStation(123L));
        assertResolvesToUrn("https://soundcloud.com/stations/artist/soundcloud:users:123", Urn.forArtistStation(123L));
        assertResolvesToUrn("soundcloud://stations/artist/soundcloud:users:123", Urn.forArtistStation(123L));
        assertResolvesToUrn("https://soundcloud.com/stations/artist/soundcloud:artist-stations:123", Urn.forArtistStation(123L));
        assertResolvesToUrn("soundcloud://stations/artist/soundcloud:artist-stations:123", Urn.forArtistStation(123L));

        assertResolvesToUrn("https://soundcloud.com/stations/track/123", Urn.forTrackStation(123L));
        assertResolvesToUrn("soundcloud://stations/track/123", Urn.forTrackStation(123L));
        assertResolvesToUrn("https://soundcloud.com/stations/track/soundcloud:tracks:123", Urn.forTrackStation(123L));
        assertResolvesToUrn("soundcloud://stations/track/soundcloud:tracks:123", Urn.forTrackStation(123L));
        assertResolvesToUrn("https://soundcloud.com/stations/track/soundcloud:track-stations:123", Urn.forTrackStation(123L));
        assertResolvesToUrn("soundcloud://stations/track/soundcloud:track-stations:123", Urn.forTrackStation(123L));
    }

    @Test
    public void testFailsToResolve() throws Exception {
        assertCannotResolve("https://soundcloud.com/stations/artist/123/213/123/123");
        assertCannotResolve("soundcloud://stations/artist/123/test");
        assertCannotResolve("https://soundcloud.com/stations/artist/soundclouds:users:123");
        assertCannotResolve("soundcloud://stations/artist/soundcloud:userssss:123");

        assertCannotResolve("https://soundcloud.com/stations/track/");
        assertCannotResolve("soundcloud://stations/track/12dgfh3");
        assertCannotResolve("https://soundcloud.com/stations/track/soundcloud:tracks");
        assertCannotResolve("soundcloud://stations/track/soundcloud:tracks:ksadjf;l123");
    }

    private void assertResolvesToUrn(String uri, Urn expectedUrn) throws UriResolveException {
        assertThat(resolver.resolve(Uri.parse(uri)).orNull()).isEqualTo(expectedUrn);
    }

    private void assertCannotResolve(String uri) throws UriResolveException {
        assertThat(resolver.resolve(Uri.parse(uri)).isPresent()).isFalse();
    }
}
