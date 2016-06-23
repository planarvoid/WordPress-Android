package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class ScrollHelperTest extends AndroidUnitTest {
    private static final int VIEW_PAGER_HEIGHT = 100;
    private static final int APP_BAR_TOTAL_RANGE = 40;

    private ScrollHelper scrollHelper;

    @Mock private AppCompatActivity activity;
    @Mock private AppBarLayout appBarLayout;
    @Mock private Toolbar toolbar;
    @Mock private ViewPager viewPager;
    @Mock private View header;

    @Captor private ArgumentCaptor<AppBarLayout.OnOffsetChangedListener> offsetChangedCaptor;
    private StubScrollScreen screen;

    @Before
    public void setUp() throws Exception {
        when(viewPager.getHeight()).thenReturn(VIEW_PAGER_HEIGHT);
        when(appBarLayout.getTotalScrollRange()).thenReturn(APP_BAR_TOTAL_RANGE);

        screen = new StubScrollScreen();
        scrollHelper = new ScrollHelper(screen);
    }

    @Test
    public void disablesSwipeToRefreshWhenOffsetLessThanZeroAfterOnStart() {
        scrollHelper.attach();

        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        offsetChangedCaptor.getValue().onOffsetChanged(appBarLayout, -1);

        assertThat(screen.isSwipeToRefreshEnabled).isFalse();
    }

    @Test
    public void enablesSwipeToRefreshWhenOffsetEqualToZeroAfterOnStart() {
        scrollHelper.attach();

        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        offsetChangedCaptor.getValue().onOffsetChanged(appBarLayout, 0);

        assertThat(screen.isSwipeToRefreshEnabled).isTrue();
    }

    @Test
    public void setsEmptyViewsToBeAvailableHeight() {
        scrollHelper.attach();

        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        final int offset = -23;
        offsetChangedCaptor.getValue().onOffsetChanged(appBarLayout, offset);

        final int availableHeight = VIEW_PAGER_HEIGHT - APP_BAR_TOTAL_RANGE - offset;
        assertThat(screen.emptyViewHeight).isEqualTo(availableHeight);
    }

    @Test
    public void removesListenerInOnStop() {
        scrollHelper.attach();
        verify(appBarLayout).addOnOffsetChangedListener(offsetChangedCaptor.capture());
        scrollHelper.detach();

        verify(appBarLayout).removeOnOffsetChangedListener(offsetChangedCaptor.getValue());
    }

    private class StubScrollScreen implements ScrollHelper.ScrollScreen {
        private boolean isSwipeToRefreshEnabled;
        private int emptyViewHeight;

        @Override
        public void setEmptyViewHeight(int height) {
            this.emptyViewHeight = height;
        }

        @Override
        public void setSwipeToRefreshEnabled(boolean enabled) {
            this.isSwipeToRefreshEnabled = enabled;
        }

        @Override
        public AppBarLayout getAppBarLayout() {
            return appBarLayout;
        }

        @Override
        public View getHeaderView() {
            return header;
        }

        @Override
        public View getContentView() {
            return viewPager;
        }

        @Override
        public Toolbar getToolbar() {
            return toolbar;
        }

        @Override
        public float getElevationTarget() {
            return 0;
        }
    }
}
