package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class ClientUriTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionOnInvalidUri() throws Exception {
        new ClientUri(Uri.parse("foo:bar:baz"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionOnInvalidString() throws Exception {
        new ClientUri("foo:bar:baz");
    }

    @Test
    public void shouldParseCorrectlyUser() throws Exception {
        ClientUri uri = new ClientUri("soundcloud:users:123");
        expect(uri.type).toEqual("users");
        expect(uri.id).toEqual("123");
        expect(uri.contentProviderUri()).toEqual(Content.USER.forId(123L));
        expect(uri.isSound()).toBeFalse();
    }

    @Test
    public void shouldParseCorrectlyTrack() throws Exception {
        ClientUri uri = new ClientUri("soundcloud:tracks:123");
        expect(uri.type).toEqual("tracks");
        expect(uri.id).toEqual("123");
        expect(uri.contentProviderUri()).toEqual(Content.TRACK.forId(123L));
        expect(uri.isSound()).toBeTrue();
    }

    @Test
    public void shouldParseCorrectlyPlaylist() throws Exception {
        ClientUri uri = new ClientUri("soundcloud:playlists:123");
        expect(uri.type).toEqual("playlists");
        expect(uri.id).toEqual("123");
        expect(uri.contentProviderUri()).toEqual(Content.PLAYLIST.forId(123L));
        expect(uri.isSound()).toBeTrue();
    }

    @Test
    public void shouldImplementEqualsAndHashCode() throws Exception {
        ClientUri uri = new ClientUri("soundcloud:tracks:123");
        ClientUri uri2 = new ClientUri("soundcloud:tracks:123");
        ClientUri uri3 = new ClientUri("soundcloud:tracks:1234");
        expect(uri).toEqual(uri2);
        expect(uri.hashCode()).toEqual(uri2.hashCode());
        expect(uri).not.toEqual(uri3);
        expect(uri.hashCode()).not.toEqual(uri3.hashCode());
    }
}
