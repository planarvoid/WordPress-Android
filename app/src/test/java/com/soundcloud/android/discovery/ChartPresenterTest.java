package com.soundcloud.android.discovery;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

public class ChartPresenterTest extends AndroidUnitTest {
    private final Urn CHART_GENRE_URN = new Urn("soundcloud:charts:rock");
    private final ChartCategory CHART_CATEGORY = ChartCategory.MUSIC;
    private final ChartType CHART_TYPE = ChartType.TOP;

    @Mock private ChartsTracker chartsTracker;
    @Mock private Bundle bundle;

    private ChartPresenter presenter;
    private Intent intent;

    @Before
    public void setUp() {
        presenter = new ChartPresenter(resources(), chartsTracker);
        intent = new Intent()
                .putExtra(ChartTracksFragment.EXTRA_GENRE_URN, CHART_GENRE_URN)
                .putExtra(ChartTracksFragment.EXTRA_TYPE, CHART_TYPE)
                .putExtra(ChartTracksFragment.EXTRA_CATEGORY, CHART_CATEGORY)
                .putExtra(ChartActivity.EXTRA_HEADER, "Header");
    }

    @Test
    public void shouldClearAndNotTrackEventOnCreate() {
        AppCompatActivity activity = createActivity();

        presenter.onCreate(activity, null);

        verify(chartsTracker).clearTracker();
        verify(chartsTracker, never()).chartPageSelected(any(Urn.class), any(ChartCategory.class), any(ChartType.class));
    }

    @Test
    public void shouldTrackEventOnResumeWhenEnteringScreen() {
        presenter.onCreate(createActivity(), null);
        final ChartActivity activity = mock(ChartActivity.class);
        when(activity.getIntent()).thenReturn(intent);
        when(activity.isEnteringScreen()).thenReturn(true);

        presenter.onResume(activity);

        verify(chartsTracker).chartPageSelected(CHART_GENRE_URN, CHART_CATEGORY, CHART_TYPE);
    }

    @Test
    public void shouldNotTrackEventOnResumeWhenNotEnteringScreen() {
        final ChartActivity activity = mock(ChartActivity.class);
        when(activity.isEnteringScreen()).thenReturn(false);

        presenter.onResume(activity);

        verifyZeroInteractions(chartsTracker);
    }

    @Test
    public void shouldTrackEventForSwitchingToAudioTab() {
        AppCompatActivity activity = createActivity();
        final ViewPager pager = (ViewPager) activity.findViewById(R.id.pager);
        presenter.onCreate(activity, bundle);
        reset(chartsTracker);

        pager.setCurrentItem(1);

        verify(chartsTracker).chartPageSelected(CHART_GENRE_URN, CHART_CATEGORY, ChartType.TOP);
    }

    @Test
    public void shouldTrackEventForSwitchingToMusicTab() {
        AppCompatActivity activity = createActivity();
        final ViewPager pager = (ViewPager) activity.findViewById(R.id.pager);
        presenter.onCreate(activity, bundle);
        pager.setCurrentItem(1);
        reset(chartsTracker);

        pager.setCurrentItem(0);

        verify(chartsTracker).chartPageSelected(CHART_GENRE_URN, CHART_CATEGORY, ChartType.TRENDING);
    }

    @NonNull
    private AppCompatActivity createActivity() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).create().get();
        final LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View inflated = layoutInflater.inflate(R.layout.tabbed_activity_content, new FrameLayout(context()), false);
        activity.setContentView(inflated);
        activity.setIntent(intent);
        return activity;
    }
}
