package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class UrnTest {

    @Test
    public void shouldValidateTrackUrnAsSoundCloudUrn() {
        expect(Urn.isSoundCloudUrn("soundcloud:tracks:1")).toBeTrue();
    }

    @Test
    public void shouldValidateLegacyTrackUrnAsSoundCloudUrn() {
        expect(Urn.isSoundCloudUrn("soundcloud:sounds:1")).toBeTrue();
    }

    @Test
    public void shouldValidatePlaylistUrnAsSoundCloudUrn() {
        expect(Urn.isSoundCloudUrn("soundcloud:playlists:1")).toBeTrue();
    }

    @Test
    public void shouldValidateUserUrnAsSoundCloudUrn() {
        expect(Urn.isSoundCloudUrn("soundcloud:users:1")).toBeTrue();
    }

    @Test
    public void shouldParseUserUrns() throws Exception {
        Urn urn = new Urn("soundcloud:users:123");
        expect(urn.isUser()).toBeTrue();
        expect(urn.getNumericId()).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.USER.forId(123L));
    }

    @Test
    public void shouldParseLegacyTrackUrns() throws Exception {
        Urn urn = new Urn("soundcloud:sounds:123");
        expect(urn.isTrack()).toBeTrue();
        expect(urn.isSound()).toBeTrue();
        expect(urn.getNumericId()).toEqual(123L);
        expect(urn.toString()).toEqual("soundcloud:tracks:123");
        expect(urn.contentProviderUri()).toEqual(Content.TRACK.forId(123L));
    }

    @Test
    public void shouldParseTrackUrns() throws Exception {
        Urn urn = new Urn("soundcloud:tracks:123");
        expect(urn.isTrack()).toBeTrue();
        expect(urn.isSound()).toBeTrue();
        expect(urn.getNumericId()).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.TRACK.forId(123L));
    }

    @Test
    public void shouldParsePlaylistUrns() throws Exception {
        Urn urn = new Urn("soundcloud:playlists:123");
        expect(urn.isPlaylist()).toBeTrue();
        expect(urn.isSound()).toBeTrue();
        expect(urn.getNumericId()).toEqual(123L);
        expect(urn.contentProviderUri()).toEqual(Content.PLAYLIST.forId(123L));
    }

    // Eventually we shouldn't have to do it anymore, but this is how we currently represent local playlists
    @Test
    public void shouldParseNegativePlaylistUrns() throws Exception {
        Urn urn = new Urn("soundcloud:playlists:-123");
        expect(urn.isPlaylist()).toBeTrue();
        expect(urn.getNumericId()).toEqual(-123L);
        expect(urn.contentProviderUri()).toEqual(Content.PLAYLIST.forId(-123L));
    }

    // This is still up for debate, but right now we represent NOT_SET Urns with numeric part -1
    @Test
    public void shouldAllowUrnsWithNegativeOneIds() {
        final Urn urn = new Urn("soundcloud:tracks:-1");
        expect(urn.getNumericId()).toEqual(-1L);
    }

    @Test
    public void shouldImplementEqualsAndHashCode() throws Exception {
        Urn uri = new Urn("soundcloud:tracks:123");
        Urn uri2 = new Urn("soundcloud:tracks:123");
        Urn uri3 = new Urn("soundcloud:tracks:1234");
        expect(uri).toEqual(uri2);
        expect(uri.hashCode()).toEqual(uri2.hashCode());
        expect(uri).not.toEqual(uri3);
        expect(uri.hashCode()).not.toEqual(uri3.hashCode());
    }

    @Test
    public void shouldBuildTrackUrns() {
        final Urn urn = Urn.forTrack(1);
        expect(urn).toEqual(new Urn("soundcloud:tracks:1"));
    }

    @Test
    public void shouldBuildPlaylistUrns() {
        final Urn urn = Urn.forPlaylist(1);
        expect(urn).toEqual(new Urn("soundcloud:playlists:1"));
    }

    @Test
    public void shouldBuildUserUrns() {
        final Urn urn = Urn.forUser(1);
        expect(urn).toEqual(new Urn("soundcloud:users:1"));
    }

    @Test
    public void shouldBuildUrnsForAnonymousUsers() {
        expect(Urn.forUser(-1).toString()).toEqual("soundcloud:users:0");
    }

    @Test
    public void shouldBeParcelable() {
        Parcel parcel = Parcel.obtain();
        Urn urn = new Urn("soundcloud:tracks:1");
        urn.writeToParcel(parcel, 0);

        Urn unparceled = Urn.CREATOR.createFromParcel(parcel);
        expect(unparceled).toEqual(urn);
    }
}
