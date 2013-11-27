package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;
import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class PlaySessionSourceTest {
    private static final long SET_ID = 123L;
    private static final Uri ORIGIN_PAGE = Uri.parse("origin:page");
    private static final TrackSourceInfo TRACK_SOURCE_INFO = TrackSourceInfo.fromSource("source", "version");

    @Test
    public void shouldCreateEmptyPlaySessionSource() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource();
        expect(playSessionSource.getOriginPage()).toBe(Uri.EMPTY);
        expect(playSessionSource.getSetId()).toEqual(-1L);
        expect(playSessionSource.getInitialTrackSourceInfo()).toEqual(TrackSourceInfo.EMPTY);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPage() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        expect(playSessionSource.getOriginPage()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getSetId()).toEqual(-1L);
        expect(playSessionSource.getInitialTrackSourceInfo()).toEqual(TrackSourceInfo.EMPTY);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageAndSetId() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE, SET_ID);
        expect(playSessionSource.getOriginPage()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getSetId()).toEqual(SET_ID);
        expect(playSessionSource.getInitialTrackSourceInfo()).toEqual(TrackSourceInfo.EMPTY);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageAndTrackSourceInfo() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE, TRACK_SOURCE_INFO);
        expect(playSessionSource.getOriginPage()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getSetId()).toEqual(-1L);
        expect(playSessionSource.getInitialTrackSourceInfo()).toBe(TRACK_SOURCE_INFO);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageTrackSourceInfoAndSetId() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE, TRACK_SOURCE_INFO, SET_ID);
        expect(playSessionSource.getOriginPage()).toBe(ORIGIN_PAGE);
        expect(playSessionSource.getSetId()).toEqual(SET_ID);
        expect(playSessionSource.getInitialTrackSourceInfo()).toBe(TRACK_SOURCE_INFO);
    }

    @Test
    public void shouldBeParcelable() throws Exception {
        PlaySessionSource original = new PlaySessionSource(ORIGIN_PAGE, TRACK_SOURCE_INFO, SET_ID);
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        PlaySessionSource copy = new PlaySessionSource(parcel);
        expect(copy.getOriginPage()).toBe(ORIGIN_PAGE);
        expect(copy.getSetId()).toEqual(SET_ID);
        expect(copy.getInitialTrackSourceInfo()).toEqual(TRACK_SOURCE_INFO);
    }
}
