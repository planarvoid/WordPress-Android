package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.images.ImageSize;
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
    public void shouldParseCorrectlySound() throws Exception {
        ClientUri uri = new ClientUri("soundcloud:sounds:123");
        expect(uri.type).toEqual("sounds");
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
        ClientUri uri = new ClientUri("soundcloud:sounds:123");
        ClientUri uri2 = new ClientUri("soundcloud:sounds:123");
        ClientUri uri3 = new ClientUri("soundcloud:sounds:1234");
        expect(uri).toEqual(uri2);
        expect(uri.hashCode()).toEqual(uri2.hashCode());
        expect(uri).not.toEqual(uri3);
        expect(uri.hashCode()).not.toEqual(uri3.hashCode());
    }

    @Test
    public void shouldBuildCorrectUriForTracks() {
        expect(ClientUri.forTrack(1).toString()).toEqual("soundcloud:sounds:1");
    }

    @Test
    public void shouldBuildCorrectUriForUsers() {
        expect(ClientUri.forUser(1).toString()).toEqual("soundcloud:users:1");
    }

    @Test
    public void shouldBuildCorrectURNForSounds() {
        expect(ClientUri.fromTrack(1)).toEqual(new ClientUri("soundcloud:sounds:1"));
    }

    @Test
    public void shouldBuildCorrectURNForUsers() {
        expect(ClientUri.fromUser(1)).toEqual(new ClientUri("soundcloud:users:1"));
    }

    @Test
    public void shouldBuildCorrectImageResolveUri() {
        expect(new ClientUri("soundcloud:sounds:123").imageUri())
                .toEqual("https://api.soundcloud.com/resolve/image?url=soundcloud%3Asounds%3A123&client_id=40ccfee680a844780a41fbe23ea89934");

    }

    @Test
    public void shouldBuildCorrectImageResolveUriWithImageSizeT500() {
        expect(new ClientUri("soundcloud:sounds:123").imageUri(ImageSize.T500))
                .toEqual("https://api.soundcloud.com/resolve/image?url=soundcloud%3Asounds%3A123&client_id=40ccfee680a844780a41fbe23ea89934&size=t500x500");

    }

}
