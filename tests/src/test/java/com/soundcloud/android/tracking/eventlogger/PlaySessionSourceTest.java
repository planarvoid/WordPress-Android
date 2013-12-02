package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.ScTextUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;
import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class PlaySessionSourceTest {
    private static final long SET_ID = 123L;
    private static final Uri ORIGIN_PAGE = Uri.parse("origin:page");
    private static final String EXPLORE_TAG = "explore:123";

    @Test
    public void shouldCreateEmptyPlaySessionSource() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource();
        expect(playSessionSource.getOriginPage()).toBe(Uri.EMPTY);
        expect(playSessionSource.getSetId()).toEqual(-1L);
        expect(playSessionSource.getInitialSource()).toEqual(ScTextUtils.EMPTY_STRING);
        expect(playSessionSource.getInitialSourceVersion()).toEqual(ScTextUtils.EMPTY_STRING);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPage() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        expect(playSessionSource.getOriginPage()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getSetId()).toEqual(-1L);
        expect(playSessionSource.getInitialSource()).toEqual(ScTextUtils.EMPTY_STRING);
        expect(playSessionSource.getInitialSourceVersion()).toEqual(ScTextUtils.EMPTY_STRING);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageAndSetId() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE, SET_ID);
        expect(playSessionSource.getOriginPage()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getSetId()).toEqual(SET_ID);
        expect(playSessionSource.getInitialSource()).toEqual(ScTextUtils.EMPTY_STRING);
        expect(playSessionSource.getInitialSourceVersion()).toEqual(ScTextUtils.EMPTY_STRING);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageAndExploreTag() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE, EXPLORE_TAG);
        expect(playSessionSource.getOriginPage()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getSetId()).toEqual(-1L);
        expect(playSessionSource.getInitialSource()).toEqual(PlaySessionSource.DiscoverySource.EXPLORE.value());
        expect(playSessionSource.getInitialSourceVersion()).toEqual(EXPLORE_TAG);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageTrackSourceInfoAndSetId() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE, SET_ID, EXPLORE_TAG);
        expect(playSessionSource.getOriginPage()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getSetId()).toEqual(SET_ID);
        expect(playSessionSource.getInitialSource()).toEqual(PlaySessionSource.DiscoverySource.EXPLORE.value());
        expect(playSessionSource.getInitialSourceVersion()).toEqual(EXPLORE_TAG);
    }

    @Test
    public void shouldBeParcelable() throws Exception {
        PlaySessionSource original = new PlaySessionSource(ORIGIN_PAGE, SET_ID, EXPLORE_TAG);
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        PlaySessionSource copy = new PlaySessionSource(parcel);
        expect(copy.getOriginPage()).toBe(ORIGIN_PAGE);
        expect(copy.getSetId()).toEqual(SET_ID);
        expect(copy.getInitialSource()).toEqual(PlaySessionSource.DiscoverySource.EXPLORE.value());
        expect(copy.getInitialSourceVersion()).toEqual(EXPLORE_TAG);
    }

    @Test
    public void shouldOriginateFromExploreWithExploreOrigin() {
        expect(new PlaySessionSource(Uri.parse("explore:something")).originatedInExplore()).toBeTrue();
    }

    @Test
    public void shouldNotOriginateFromExploreWithNonExploreOrigin() {
        expect(new PlaySessionSource(Uri.parse("stream:something")).originatedInExplore()).toBeFalse();
    }

    @Test
    public void shouldNotOriginateFromExploreWithEmptyOrigin() {
        expect(new PlaySessionSource(Uri.EMPTY).originatedInExplore()).toBeFalse();
    }
}
