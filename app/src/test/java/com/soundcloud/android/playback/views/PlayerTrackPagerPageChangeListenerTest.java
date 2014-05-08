package com.soundcloud.android.playback.views;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlayerTrackPagerAdapter;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.view.ViewPager;

@RunWith(SoundCloudTestRunner.class)
public class PlayerTrackPagerPageChangeListenerTest {

    PlayerTrackPager.PageChangeListener pageChangeListener;

    @Mock
    PlayerTrackPager playerTrackPager;
    @Mock
    PlayerTrackPagerAdapter playerTrackPagerAdapter;
    @Mock
    LegacyPlayerTrackView playerTrackView;
    @Mock
    PlayerTrackPager.OnTrackPageListener trackPageListener;

    @Before
    public void setup(){
        pageChangeListener = new PlayerTrackPager.PageChangeListener(playerTrackPager);
    }

    @Test
    public void shouldTellTheNextTrackPagerItIsNowOnScreen() throws Exception {
        when(playerTrackPager.getCurrentItem()).thenReturn(2);
        when(playerTrackPager.getAdapter()).thenReturn(playerTrackPagerAdapter);
        when(playerTrackPagerAdapter.getPlayerTrackViewByPosition(3)).thenReturn(playerTrackView);
        pageChangeListener.onPageScrolled(2, 5, 5);
        verify(playerTrackView).setOnScreen(true);
    }

    @Test
    public void shouldTellThePreviousTrackPagerItIsNowOnScreen() throws Exception {
        when(playerTrackPager.getCurrentItem()).thenReturn(2);
        when(playerTrackPager.getAdapter()).thenReturn(playerTrackPagerAdapter);
        when(playerTrackPagerAdapter.getPlayerTrackViewByPosition(1)).thenReturn(playerTrackView);
        pageChangeListener.onPageScrolled(1, 5, 5);
        verify(playerTrackView).setOnScreen(true);
    }

    @Test
    public void onPageScrollStateChangedShouldCallOnPageDragWhenDragging() throws Exception {
        pageChangeListener.setTrackPageListener(trackPageListener);
        pageChangeListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_DRAGGING);
        verify(trackPageListener).onPageDrag();
    }

    @Test
    public void onPageScrollStateChangedShouldCallOnPageChangedWhenSettling() throws Exception {
        pageChangeListener.setTrackPageListener(trackPageListener);
        pageChangeListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_SETTLING);
        verify(trackPageListener).onPageChanged();
    }

    @Test
    public void onPageScrollStateChangedShouldCallOnPageChangedWhenIdleAfterDragging() throws Exception {
        pageChangeListener.setTrackPageListener(trackPageListener);
        pageChangeListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_DRAGGING);
        pageChangeListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        verify(trackPageListener).onPageChanged();
    }

    @Test
    public void onPageScrollStateChangedShouldNotCallOnPageChangedWhenIdleAfterSettling() throws Exception {
        pageChangeListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_SETTLING);
        pageChangeListener.setTrackPageListener(trackPageListener);
        pageChangeListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        verify(trackPageListener, never()).onPageChanged();
    }
}
