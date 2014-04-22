package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        expect(urn).toBeInstanceOf(UserUrn.class);
        expect(urn.type).toEqual("users");
        expect(urn.numericId).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.USER.forId(123L));
        expect(urn.isSound()).toBeFalse();
    }

    @Test
    public void shouldParseLegacyTrackUrns() throws Exception {
        Urn urn = Urn.parse("soundcloud:sounds:123");
        expect(urn).toBeInstanceOf(TrackUrn.class);
        expect(urn.type).toEqual("sounds");
        expect(urn.numericId).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.TRACK.forId(123L));
        expect(urn.isSound()).toBeTrue();
    }

    @Test
    public void shouldParseTrackUrns() throws Exception {
        Urn urn = Urn.parse("soundcloud:tracks:123");
        expect(urn).toBeInstanceOf(TrackUrn.class);
        expect(urn.type).toEqual("sounds"); // TODO: should move to "tracks"
        expect(urn.numericId).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.TRACK.forId(123L));
        expect(urn.isSound()).toBeTrue();
    }

    @Test
    public void shouldParsePlaylistUrns() throws Exception {
        Urn urn = Urn.parse("soundcloud:playlists:123");
        expect(urn).toBeInstanceOf(PlaylistUrn.class);
        expect(urn.type).toEqual("playlists");
        expect(urn.numericId).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.PLAYLIST.forId(123L));
        expect(urn.isSound()).toBeTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenTryingToParseInvalidUrn() {
        Urn.parse("not a URN");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenNotASupportedType() {
        Urn.parse("soundcloud:something:1");
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
        expect(urn).toBeInstanceOf(TrackUrn.class);
        expect(urn).toEqual(Urn.parse("soundcloud:sounds:1"));
    }

    @Test
    public void shouldBuildPlaylistUrns() {
        final Urn urn = Urn.forPlaylist(1);
        expect(urn).toBeInstanceOf(PlaylistUrn.class);
        expect(urn).toEqual(Urn.parse("soundcloud:playlists:1"));
    }

    @Test
    public void shouldBuildUserUrns() {
        final Urn urn = Urn.forUser(1);
        expect(urn).toBeInstanceOf(UserUrn.class);
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
