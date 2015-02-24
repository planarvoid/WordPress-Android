package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.ScTextUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class PlaySessionSourceTest {
    private static final String ORIGIN_PAGE = "origin:page";
    private static final String EXPLORE_TAG = "explore:123";
    private PublicApiPlaylist playlist;

    @Before
    public void setUp() throws Exception {
        playlist = ModelFixtures.create(PublicApiPlaylist.class);
    }

    @Test
    public void shouldCreateEmptyPlaySessionSource() throws Exception {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        expect(playSessionSource.getOriginScreen()).toBe(ScTextUtils.EMPTY_STRING);
        expect(playSessionSource.getPlaylistUrn()).toEqual(Urn.NOT_SET);
        expect(playSessionSource.getPlaylistOwnerUrn()).toEqual(Urn.NOT_SET);
        expect(playSessionSource.getInitialSource()).toEqual(ScTextUtils.EMPTY_STRING);
        expect(playSessionSource.getInitialSourceVersion()).toEqual(ScTextUtils.EMPTY_STRING);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPage() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        expect(playSessionSource.getOriginScreen()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getPlaylistUrn()).toEqual(Urn.NOT_SET);
        expect(playSessionSource.getPlaylistOwnerUrn()).toEqual(Urn.NOT_SET);
        expect(playSessionSource.getInitialSource()).toEqual(ScTextUtils.EMPTY_STRING);
        expect(playSessionSource.getInitialSourceVersion()).toEqual(ScTextUtils.EMPTY_STRING);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageAndSetId() throws Exception {

        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        expect(playSessionSource.getOriginScreen()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getPlaylistUrn()).toEqual(playlist.getUrn());
        expect(playSessionSource.getPlaylistOwnerUrn()).toEqual(playlist.getUserUrn());
        expect(playSessionSource.getInitialSource()).toEqual(ScTextUtils.EMPTY_STRING);
        expect(playSessionSource.getInitialSourceVersion()).toEqual(ScTextUtils.EMPTY_STRING);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageAndExploreTag() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setExploreVersion(EXPLORE_TAG);
        expect(playSessionSource.getOriginScreen()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getPlaylistUrn()).toEqual(Urn.NOT_SET);
        expect(playSessionSource.getPlaylistOwnerUrn()).toEqual(Urn.NOT_SET);
        expect(playSessionSource.getInitialSource()).toEqual(PlaySessionSource.DiscoverySource.EXPLORE.value());
        expect(playSessionSource.getInitialSourceVersion()).toEqual(EXPLORE_TAG);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageTrackSourceInfoAndSetId() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setExploreVersion(EXPLORE_TAG);
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        expect(playSessionSource.getOriginScreen()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getPlaylistUrn()).toEqual(playlist.getUrn());
        expect(playSessionSource.getPlaylistOwnerUrn()).toEqual(playlist.getUserUrn());
        expect(playSessionSource.getInitialSource()).toEqual(PlaySessionSource.DiscoverySource.EXPLORE.value());
        expect(playSessionSource.getInitialSourceVersion()).toEqual(EXPLORE_TAG);
    }

    @Test
    public void shouldBeParcelable() throws Exception {
        PlaySessionSource original = new PlaySessionSource(ORIGIN_PAGE);
        original.setExploreVersion(EXPLORE_TAG);
        original.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        PlaySessionSource copy = new PlaySessionSource(parcel);
        expect(copy.getOriginScreen()).toBe(ORIGIN_PAGE);
        expect(copy.getPlaylistUrn()).toEqual(playlist.getUrn());
        expect(copy.getPlaylistOwnerUrn()).toEqual(playlist.getUserUrn());
        expect(copy.getInitialSource()).toEqual(PlaySessionSource.DiscoverySource.EXPLORE.value());
        expect(copy.getInitialSourceVersion()).toEqual(EXPLORE_TAG);
    }

    @Test
    public void shouldOriginateFromExploreWithExploreOrigin() {
        expect(new PlaySessionSource("explore:something").originatedInExplore()).toBeTrue();
    }

    @Test
    public void shouldNotOriginateFromExploreWithNonExploreOrigin() {
        expect(new PlaySessionSource("stream:something").originatedInExplore()).toBeFalse();
    }

    @Test
    public void shouldNotOriginateFromExploreWithEmptyOrigin() {
        expect(new PlaySessionSource(ScTextUtils.EMPTY_STRING).originatedInExplore()).toBeFalse();
    }
}
