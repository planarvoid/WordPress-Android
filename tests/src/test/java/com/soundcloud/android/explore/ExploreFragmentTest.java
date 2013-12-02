package com.soundcloud.android.explore;


import static com.soundcloud.android.explore.ExploreFragment.ExplorePagerScreenListener;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.Event;
import com.viewpagerindicator.TabPageIndicator;
import com.xtremelabs.robolectric.Robolectric;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

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
    @Mock
    private Observer observer;

    @Inject
    ExploreFragment mExploreFragment;

    @Before
    public void setUp() throws Exception {
        ObjectGraph.create(new TestModule()).inject(this);

        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.indicator)).thenReturn(mockIndicator);

        mExploreFragment.onCreate(null);
    }

    @Test
    public void shouldDefaultToTabCategories() {
        mExploreFragment.onViewCreated(mockLayout, null);
        verify(mockViewPager).setCurrentItem(ExplorePagerAdapter.TAB_GENRES);
    }

    @Test
    public void shouldNullOutPagerAdapterWhenDestroyingViewsToPreventContextLeaks() {
        mExploreFragment.onViewCreated(mockLayout, null);
        mExploreFragment.onDestroyView();
        verify(mockViewPager).setAdapter(null);
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents(){
        mExploreFragment.onViewCreated(mockLayout, null);
        verify(mockViewPager).setOnPageChangeListener(isA(ExplorePagerScreenListener.class));
    }

    @Test
    public void shouldTrackGenresScreenOnPageSelected() {
        Event.SCREEN_ENTERED.subscribe(observer);
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener();
        explorePagerScreenListener.onPageSelected(0);
        verify(observer).onNext("explore:genres");
        verifyNoMoreInteractions(observer);

    }

    @Test
    public void shouldTrackTrendingMusicScreenOnPageSelected() {
        Event.SCREEN_ENTERED.subscribe(observer);
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener();
        explorePagerScreenListener.onPageSelected(1);
        verify(observer).onNext("explore:trending_music");
        verifyNoMoreInteractions(observer);

    }

    @Test
    public void shouldTrackTrendingAudioScreenOnPageSelected() {
        Event.SCREEN_ENTERED.subscribe(observer);
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener();
        explorePagerScreenListener.onPageSelected(2);
        verify(observer).onNext("explore:trending_audio");
        verifyNoMoreInteractions(observer);

    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnknownScreenIsViewed() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener();
        explorePagerScreenListener.onPageSelected(3);

    }
}
