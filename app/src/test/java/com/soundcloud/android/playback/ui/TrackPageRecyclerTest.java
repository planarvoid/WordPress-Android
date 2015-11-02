package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;

public class TrackPageRecyclerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);
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

        assertThat(trackPageRecycler.hasExistingPage(TRACK_URN)).isTrue();
    }

    @Test
    public void getRecycledPageReturnsCachedPage() throws Exception {
        trackPageRecycler.addScrapView(view);
        trackPageRecycler.recyclePage(TRACK_URN, view);

        assertThat(trackPageRecycler.getRecycledPage()).isSameAs(view);
    }

    @Test
    public void recyclePageShouldAllowDuplicates() {
        trackPageRecycler.recyclePage(TRACK_URN, view);
        trackPageRecycler.recyclePage(TRACK_URN, view2);

        assertThat(trackPageRecycler.getRecycledPage()).isSameAs(view);
        assertThat(trackPageRecycler.getRecycledPage()).isSameAs(view2);
    }

    @Test
    public void getRecycledPageReturnsLastPageCachedByUrn() throws Exception {
        trackPageRecycler.recyclePage(TRACK_URN, view);
        trackPageRecycler.recyclePage(TRACK_URN2, view2);

        assertThat(trackPageRecycler.getRecycledPage()).isSameAs(view);
    }

    @Test
    public void getPageByUrnReusesPageCachedByUrn() throws Exception {
        trackPageRecycler.recyclePage(TRACK_URN, view);
        trackPageRecycler.recyclePage(TRACK_URN2, view2);

        assertThat(trackPageRecycler.removePageByUrn(TRACK_URN2)).isSameAs(view2);
    }

    @Test
    public void isPageForUrnReturnsPageByUrn() throws Exception {
        trackPageRecycler.recyclePage(TRACK_URN, view);

        assertThat(trackPageRecycler.isPageForUrn(view, TRACK_URN)).isTrue();
    }
}
