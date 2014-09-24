package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;
import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class UrnTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionOnUnsupportedUrn() throws Exception {
        Urn.parse("foo:bar:baz");
    }

    @Test
    public void shouldParseUserUrns() throws Exception {
        Urn urn = Urn.parse("soundcloud:users:123");
        expect(urn.isUser()).toBeTrue();
        expect(urn.numericId).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.USER.forId(123L));
    }

    @Test
    public void shouldParseLegacyTrackUrns() throws Exception {
        Urn urn = Urn.parse("soundcloud:sounds:123");
        expect(urn.isTrack()).toBeTrue();
        expect(urn.isSound()).toBeTrue();
        expect(urn.numericId).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.TRACK.forId(123L));
    }

    @Test
    public void shouldParseTrackUrns() throws Exception {
        Urn urn = Urn.parse("soundcloud:tracks:123");
        expect(urn.isTrack()).toBeTrue();
        expect(urn.isSound()).toBeTrue();
        expect(urn.numericId).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.TRACK.forId(123L));
    }

    @Test
    public void shouldParsePlaylistUrns() throws Exception {
        Urn urn = Urn.parse("soundcloud:playlists:123");
        expect(urn.isPlaylist()).toBeTrue();
        expect(urn.isSound()).toBeTrue();
        expect(urn.numericId).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.PLAYLIST.forId(123L));
    }

    // Eventually we shouldn't have to do it anymore, but this is how we currently represent local playlists
    @Test
    public void shouldParseNegativePlaylistUrns() throws Exception {
        Urn urn = Urn.parse("soundcloud:playlists:-123");
        expect(urn.isPlaylist()).toBeTrue();
        expect(urn.numericId).toEqual(-123L);
        expect(urn.contentProviderUri()).toEqual(Content.PLAYLIST.forId(-123L));
    }

    // This is still up for debate, but right now we represent NOT_SET Urns with numeric part -1
    @Test
    public void shouldAllowUrnsWithNegativeOneIds() {
        final Urn urn = Urn.parse("soundcloud:tracks:-1");
        expect(urn.numericId).toEqual(-1L);
    }

    @Test
    public void isValidUrnShouldReturnTrueForValidSoundUrn() throws Exception {
        expect(Urn.isValidUrn(Uri.parse("soundcloud:sounds:123"))).toBeTrue();
        expect(Urn.isValidUrn(Uri.parse("soundcloud:tracks:123"))).toBeTrue();
    }

    @Test
    public void isValidUrnShouldReturnTrueForValidUserUrn() throws Exception {
        expect(Urn.isValidUrn(Uri.parse("soundcloud:users:123"))).toBeTrue();
    }

    @Test
    public void isValidUrnShouldReturnTrueForValidPlaylistUrn() throws Exception {
        expect(Urn.isValidUrn(Uri.parse("soundcloud:playlists:123"))).toBeTrue();
    }

    @Test
    public void isUrnReturnsFalseForInvalidUrn() {
        expect(Urn.isValidUrn(Uri.parse("not a URN"))).toBeFalse();
    }

    @Test
    public void isUrnReturnsFalseIfSchemeIsNotSoundCloudScheme() {
        expect(Urn.isValidUrn(Uri.parse("something:users:213"))).toBeFalse();
    }

    @Test
    public void isValidUrnReturnsFalseIfIdentifierIsUnsupported() {
        expect(Urn.isValidUrn(Uri.parse("soundcloud:sounds:abc"))).toBeFalse();
    }

    @Test
    public void isValidUrnReturnsFalseIfNamespaceNotSupported() {
        expect(Urn.isValidUrn(Uri.parse("soundcloud:something:1"))).toBeFalse();
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
    public void shouldBuildTrackUrns() {
        final Urn urn = Urn.forTrack(1);
        expect(urn).toEqual(Urn.parse("soundcloud:sounds:1"));
    }

    @Test
    public void shouldBuildPlaylistUrns() {
        final Urn urn = Urn.forPlaylist(1);
        expect(urn).toEqual(Urn.parse("soundcloud:playlists:1"));
    }

    @Test
    public void shouldBuildUserUrns() {
        final Urn urn = Urn.forUser(1);
        expect(urn).toEqual(Urn.parse("soundcloud:users:1"));
    }

    @Test
    public void shouldBuildUrnsForAnonymousUsers() {
        expect(Urn.forUser(-1).toString()).toEqual("soundcloud:users:0");
    }

    @Test
    public void shouldBeParcelable() {
        Parcel parcel = Parcel.obtain();
        Urn urn = Urn.parse("soundcloud:tracks:1");
        urn.writeToParcel(parcel, 0);

        Urn unparceled = Urn.CREATOR.createFromParcel(parcel);
        expect(unparceled).toEqual(urn);
    }
}
