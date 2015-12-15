package com.soundcloud.android.utils;

import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.view.ParallaxImageView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

@RunWith(MockitoJUnitRunner.class)
public class AbsListViewParallaxerTest {

    private AbsListViewParallaxer absListViewParallaxer;

    @Mock private AbsListView.OnScrollListener onScrollListener;
    @Mock private AbsListView absListView;
    @Mock private View view;
    @Mock private ViewGroup viewGroup;
    @Mock private Resources resources;

    @Before
    public void setUp() throws Exception {
        absListViewParallaxer = new AbsListViewParallaxer(onScrollListener);
        when(absListView.getHeight()).thenReturn(100);
        when(absListView.getChildCount()).thenReturn(1);
        when(absListView.getChildAt(0)).thenReturn(viewGroup);
        when(viewGroup.getChildCount()).thenReturn(1);
        when(viewGroup.getChildAt(0)).thenReturn(view);

        when(view.getTop()).thenReturn(10);
        when(view.getBottom()).thenReturn(30);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics.density = 1;
        when(absListView.getResources()).thenReturn(resources);
        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);
    }

    @Test
    public void shouldCallScrollDelegateOnScroll() throws Exception {
        absListViewParallaxer.onScrollStateChanged(absListView, 2);
        verify(onScrollListener).onScrollStateChanged(absListView, 2);

        absListViewParallaxer.onScroll(absListView, 1, 2, 3);
        verify(onScrollListener).onScroll(absListView, 1, 2, 3);
    }

    @Test
    public void shouldNotDoAnythingWhenListHasNoHeight() throws Exception {
        when(absListView.getHeight()).thenReturn(0);
        absListViewParallaxer.onScroll(absListView, 0, 0, 0);
        verifyZeroInteractions(viewGroup);
    }

    @Test
    public void shouldNotApplyParallaxWithNoTaggedViews() throws Exception {
        absListViewParallaxer.onScroll(absListView, 0, 0, 0);
        verify(view, never()).setTranslationY(anyFloat());
    }

    @Test
    public void shouldApplyImageParallax() throws Exception {
        ParallaxImageView parallaxImageView = Mockito.mock(ParallaxImageView.class);
        when(viewGroup.getChildAt(0)).thenReturn(parallaxImageView);

        absListViewParallaxer.onScroll(absListView, 0, 0, 0);
        verify(parallaxImageView).setParallaxOffset(-1.0f);
    }

    @Test
    public void shouldApplyForegroundParallax() throws Exception {
        when(view.getTag()).thenReturn(AbsListViewParallaxer.VIEW_FOREGROUND_TAG);
        absListViewParallaxer.onScroll(absListView, 0, 0, 0);
        verify(view).setTranslationY(6.0f);
    }

}
