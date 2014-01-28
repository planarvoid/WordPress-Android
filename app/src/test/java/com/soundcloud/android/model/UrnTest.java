package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class UrnTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionOnUnsupportedUrn() throws Exception {
        Urn.parse("foo:bar:baz");
    }

    @Test
    public void shouldParseUserUrns() throws Exception {
        Urn uri = Urn.parse("soundcloud:users:123");
        expect(uri.type).toEqual("users");
        expect(uri.id).toEqual("123");
        expect(uri.contentProviderUri()).toEqual(Content.USER.forId(123L));
        expect(uri.isSound()).toBeFalse();
    }

    @Test
    public void shouldParseSoundUrns() throws Exception {
        Urn uri = Urn.parse("soundcloud:sounds:123");
        expect(uri.type).toEqual("sounds");
        expect(uri.id).toEqual("123");
        expect(uri.contentProviderUri()).toEqual(Content.TRACK.forId(123L));
        expect(uri.isSound()).toBeTrue();
    }

    @Test
    public void shouldParsePlaylistUrns() throws Exception {
        Urn uri = Urn.parse("soundcloud:playlists:123");
        expect(uri.type).toEqual("playlists");
        expect(uri.id).toEqual("123");
        expect(uri.contentProviderUri()).toEqual(Content.PLAYLIST.forId(123L));
        expect(uri.isSound()).toBeTrue();
    }

    @Test
    public void shouldImplementEqualsAndHashCode() throws Exception {
        Urn uri = Urn.parse("soundcloud:sounds:123");
        Urn uri2 = Urn.parse("soundcloud:sounds:123");
        Urn uri3 = Urn.parse("soundcloud:sounds:1234");
        expect(uri).toEqual(uri2);
        expect(uri.hashCode()).toEqual(uri2.hashCode());
        expect(uri).not.toEqual(uri3);
        expect(uri.hashCode()).not.toEqual(uri3.hashCode());
    }

    @Test
    public void shouldBuildTrackUris() {
        expect(Urn.forTrack(1)).toEqual(Urn.parse("soundcloud:sounds:1"));
    }

    @Test
    public void shouldBuildUserUris() {
        expect(Urn.forUser(1)).toEqual(Urn.parse("soundcloud:users:1"));
    }

    @Test
    public void shouldBuildUrnsForAnonymousUsers() {
        expect(Urn.forUser(-1).toString()).toEqual("soundcloud:users:0");
    }

    @Test
    public void shouldBuildImageResolveUri() {
        expect(Urn.parse("soundcloud:sounds:123").imageUri())
                .toEqual("https://api.soundcloud.com/resolve/image?url=soundcloud%3Asounds%3A123&client_id=40ccfee680a844780a41fbe23ea89934");

    }

    @Test
    public void shouldBuildImageResolveUriWithImageSizeT500() {
        expect(Urn.parse("soundcloud:sounds:123").imageUri(ImageSize.T500))
                .toEqual("https://api.soundcloud.com/resolve/image?url=soundcloud%3Asounds%3A123&client_id=40ccfee680a844780a41fbe23ea89934&size=t500x500");

    }

}
