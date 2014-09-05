package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackUrn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class TrackPageRecyclerTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);
    private static final TrackUrn TRACK_URN2 = Urn.forTrack(456L);
    private TrackPageRecycler trackPageRecycler;

    @Mock private View view;
    @Mock private View view2;

    @Before
    public void setUp() throws Exception {
        trackPageRecycler = new TrackPageRecycler();
    }

    @Test
    public void hasExistingPageReturnsTrueAfterCachingPage() throws Exception {
        trackPageRecycler.recyclePage(TRACK_URN, view);

        expect(trackPageRecycler.hasExistingPage(TRACK_URN)).toBeTrue();
    }

    @Test
    public void getRecycledPageReturnsCachedPage() throws Exception {
        trackPageRecycler.addScrapView(view);
        trackPageRecycler.recyclePage(TRACK_URN, view);

        expect(trackPageRecycler.getRecycledPage()).toBe(view);
    }

    @Test
    public void getRecycledPageReturnsLastPageCachedByUrn() throws Exception {
        trackPageRecycler.recyclePage(TRACK_URN, view);
        trackPageRecycler.recyclePage(TRACK_URN2, view2);

        expect(trackPageRecycler.getRecycledPage()).toBe(view);
    }

    @Test
    public void getPageByUrnReusesPageCachedByUrn() throws Exception {
        trackPageRecycler.recyclePage(TRACK_URN, view);
        trackPageRecycler.recyclePage(TRACK_URN2, view2);

        expect(trackPageRecycler.getPageByUrn(TRACK_URN2)).toBe(view2);
    }
}