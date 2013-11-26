package com.soundcloud.android.explore;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.viewpagerindicator.TabPageIndicator;
import com.xtremelabs.robolectric.Robolectric;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.support.v4.view.ViewPager;
import android.view.View;

import javax.inject.Inject;

@RunWith(SoundCloudTestRunner.class)
public class ExploreFragmentTest {

    @Module(injects = ExploreFragmentTest.class)
    public class TestModule {
        @Provides
        public Resources provideResources() {
            return Robolectric.application.getResources();
        }
    }

    @Mock
    private View mockLayout;
    @Mock
    private TabPageIndicator mockIndicator;
    @Mock
    private ViewPager mockViewPager;

    @Inject
    ExploreFragment mExploreFragment;

    @Before
    public void setUp() throws Exception {
        ObjectGraph.create(new TestModule()).inject(this);

        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.indicator)).thenReturn(mockIndicator);

        mExploreFragment.onCreate(null);
        mExploreFragment.onViewCreated(mockLayout, null);
    }

    @Test
    public void shouldDefaultToTabCategories() {
        verify(mockViewPager).setCurrentItem(ExplorePagerAdapter.TAB_CATEGORIES);
    }

    @Test
    public void shouldNullOutPagerAdapterWhenDestroyingViewsToPreventContextLeaks() {
        mExploreFragment.onDestroyView();
        verify(mockViewPager).setAdapter(null);
    }
}
