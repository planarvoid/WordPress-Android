package com.soundcloud.android.explore;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class ExplorePresenterTest {

    @Mock private Resources resources;
    @Mock private ExplorePagerAdapterFactory adapterFactory;
    @Mock private ExplorePagerScreenListener screenListener;
    @Mock private AppCompatActivity activity;
    @Mock private ExplorePagerAdapter pagerAdapter;
    @Mock private TabLayout tabLayout;
    @Mock private ViewPager viewPager;

    private ExplorePresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(activity.findViewById(R.id.pager)).thenReturn(viewPager);
        when(activity.findViewById(R.id.tab_indicator)).thenReturn(tabLayout);

        presenter = new ExplorePresenter(resources, adapterFactory, screenListener);
    }

    @Test
    public void setsDefaultPageOnFirstCreate() {
        presenter.onCreate(activity, null);

        verify(viewPager).setCurrentItem(1);
    }

    @Test
    public void doesNotSetDefaultPageAfterFirstCrate() {
        presenter.onCreate(activity, new Bundle());

        verify(viewPager, never()).setCurrentItem(1);
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents() {
        presenter.onCreate(activity, null);

        verify(viewPager).addOnPageChangeListener(screenListener);
    }

}
