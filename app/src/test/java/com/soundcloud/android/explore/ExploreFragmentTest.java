package com.soundcloud.android.explore;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;

public class ExploreFragmentTest extends AndroidUnitTest {

    @Mock private View mockLayout;
    @Mock private TabLayout mockTabLayout;
    @Mock private ViewPager mockViewPager;
    @Mock private ExplorePagerAdapterFactory adapterFactory;
    @Mock private ExplorePagerAdapter pagerAdapter;
    @Mock private ExplorePagerScreenListener screenListener;

    private ExploreFragment fragment;

    @Before
    public void setUp() throws Exception {
        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.tab_indicator)).thenReturn(mockTabLayout);
        when(adapterFactory.create(any(FragmentManager.class))).thenReturn(pagerAdapter);

        fragment = new ExploreFragment(resources(), adapterFactory, screenListener);

        fragment.onCreate(null);
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents() {
        when(mockLayout.findViewById(R.id.tab_indicator)).thenReturn(mockTabLayout);
        fragment.onViewCreated(mockLayout, null);
        verify(mockViewPager).addOnPageChangeListener(isA(ExplorePagerScreenListener.class));
    }

}
