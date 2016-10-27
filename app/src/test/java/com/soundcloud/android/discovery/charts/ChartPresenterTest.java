package com.soundcloud.android.discovery.charts;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.main.EnterScreenDispatcher;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

public class ChartPresenterTest extends AndroidUnitTest {
    private final Urn CHART_GENRE_URN = new Urn("soundcloud:charts:rock");
    private final ChartCategory CHART_CATEGORY = ChartCategory.MUSIC;
    private final ChartType CHART_TYPE = ChartType.TOP;

    @Mock private ChartsTracker chartsTracker;
    @Mock private Bundle bundle;
    @Mock EnterScreenDispatcher enterScreenDispatcher;
    @Mock RootActivity rootActivity;
    @Mock private TabLayout tabLayout;
    private ViewPager viewPager;

    private ChartPresenter presenter;
    private Intent intent;

    @Before
    public void setUp() {
        viewPager = new ViewPager(context());
        presenter = new ChartPresenter(resources(), chartsTracker, enterScreenDispatcher);
        intent = new Intent()
                .putExtra(ChartTracksFragment.EXTRA_GENRE_URN, CHART_GENRE_URN)
                .putExtra(ChartTracksFragment.EXTRA_TYPE, CHART_TYPE)
                .putExtra(ChartTracksFragment.EXTRA_CATEGORY, CHART_CATEGORY)
                .putExtra(ChartActivity.EXTRA_HEADER, "Header");

        when(rootActivity.getIntent()).thenReturn(intent);
        when(rootActivity.findViewById(R.id.pager)).thenReturn(viewPager);
        when(rootActivity.findViewById(R.id.tab_indicator)).thenReturn(tabLayout);
    }

    @Test
    public void shouldClearAndNotTrackEventOnCreate() {
        presenter.onCreate(rootActivity, null);

        verify(chartsTracker).clearTracker();
        verify(chartsTracker, never()).chartPageSelected(any(Urn.class), any(ChartCategory.class), any(ChartType.class));
    }

    @Test
    public void shouldTrackEventOnResumeWhenEnteringScreen() {
        presenter.onCreate(rootActivity, null);
        final ChartActivity activity = mock(ChartActivity.class);
        when(activity.getIntent()).thenReturn(intent);

        presenter.onEnterScreen(activity);

        verify(chartsTracker).chartPageSelected(CHART_GENRE_URN, CHART_CATEGORY, CHART_TYPE);
    }

    @Test
    public void shouldTrackEventForSwitchingToAudioTab() {
        presenter.onCreate(rootActivity, bundle);
        reset(chartsTracker);

        viewPager.setCurrentItem(1);

        verify(chartsTracker).chartPageSelected(CHART_GENRE_URN, CHART_CATEGORY, ChartType.TOP);
    }

    @Test
    public void shouldTrackEventForSwitchingToMusicTab() {
        presenter.onCreate(rootActivity, bundle);
        viewPager.setCurrentItem(1);
        reset(chartsTracker);

        viewPager.setCurrentItem(0);

        verify(chartsTracker).chartPageSelected(CHART_GENRE_URN, CHART_CATEGORY, ChartType.TRENDING);
    }
}
