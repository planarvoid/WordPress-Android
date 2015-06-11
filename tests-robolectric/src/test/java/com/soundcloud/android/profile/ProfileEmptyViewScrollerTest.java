package com.soundcloud.android.profile;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class ProfileEmptyViewScrollerTest {

    ProfileEmptyViewScroller scroller;

    @Mock private Resources resources;
    @Mock private EmptyView emptyView;

    @Before
    public void setUp() throws Exception {
        when(resources.getDimensionPixelSize(R.dimen.profile_header_expanded_height)).thenReturn(20);
        scroller = new ProfileEmptyViewScroller(resources);
    }

    @Test
    public void setsMaximumIfNoTopHeightPreviouslyConfigured() throws Exception {
        scroller.setView(emptyView);

        verify(emptyView).setPadding(anyInt(), eq(20), anyInt(), anyInt());
    }

    @Test
    public void configuresTopOffsetsAfterSetView() throws Exception {
        scroller.setView(emptyView);
        scroller.configureOffsets(10);

        verify(emptyView).setPadding(anyInt(), eq(10), anyInt(), anyInt());
    }

    @Test
    public void configuresTopOffsetsAfterSetViewIfConfiguredBefore() throws Exception {
        scroller.configureOffsets(10);
        scroller.setView(emptyView);

        verify(emptyView).setPadding(anyInt(), eq(10), anyInt(), anyInt());
    }
}