package com.soundcloud.android.explore;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.explore.ExploreFragment.ExplorePagerScreenListener;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.SlidingTabLayout;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class ExploreFragmentTest {

    @Mock
    private View mockLayout;
    @Mock
    private SlidingTabLayout mockTabLayout;
    @Mock
    private ViewPager mockViewPager;
    @Mock
    private ExplorePagerAdapterFactory adapterFactory;
    @Mock
    private ExplorePagerAdapter pagerAdapter;
    @Mock
    private EventBus eventBus;

    private ExploreFragment mExploreFragment;
    private EventMonitor eventMonitor;

    @Before
    public void setUp() throws Exception {
        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.indicator)).thenReturn(mockTabLayout);
        when(adapterFactory.create(any(FragmentManager.class))).thenReturn(pagerAdapter);

        eventMonitor = EventMonitor.on(eventBus);

        mExploreFragment = new ExploreFragment(adapterFactory, eventBus);
        Robolectric.shadowOf(mExploreFragment).setActivity(new FragmentActivity());
        Robolectric.shadowOf(mExploreFragment).setAttached(true);

        mExploreFragment.onCreate(null);
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents() {
        when(mockLayout.findViewById(R.id.sliding_tabs)).thenReturn(mockTabLayout);
        mExploreFragment.onViewCreated(mockLayout, null);
        verify(mockTabLayout).setOnPageChangeListener(isA(ExplorePagerScreenListener.class));
    }

    @Test
    public void shouldTrackGenresScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(0);
        String screenTag = eventMonitor.verifyEventOn(EventQueue.SCREEN_ENTERED);
        expect(screenTag).toEqual("explore:genres");
    }

    @Test
    public void shouldTrackTrendingMusicScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(1);
        String screenTag = eventMonitor.verifyEventOn(EventQueue.SCREEN_ENTERED);
        expect(screenTag).toEqual("explore:trending_music");
    }

    @Test
    public void shouldTrackTrendingAudioScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(2);
        String screenTag = eventMonitor.verifyEventOn(EventQueue.SCREEN_ENTERED);
        expect(screenTag).toEqual("explore:trending_audio");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnknownScreenIsViewed() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(3);
    }
}
