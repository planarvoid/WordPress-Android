package com.soundcloud.android.explore;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.explore.ExploreFragment.ExplorePagerScreenListener;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
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

    @Mock private View mockLayout;
    @Mock private SlidingTabLayout mockTabLayout;
    @Mock private ViewPager mockViewPager;
    @Mock private ExplorePagerAdapterFactory adapterFactory;
    @Mock private ExplorePagerAdapter pagerAdapter;

    private ExploreFragment fragment;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.indicator)).thenReturn(mockTabLayout);
        when(adapterFactory.create(any(FragmentManager.class))).thenReturn(pagerAdapter);

        fragment = new ExploreFragment(adapterFactory, eventBus);
        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());
        Robolectric.shadowOf(fragment).setAttached(true);

        fragment.onCreate(null);
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents() {
        when(mockLayout.findViewById(R.id.sliding_tabs)).thenReturn(mockTabLayout);
        fragment.onViewCreated(mockLayout, null);
        verify(mockTabLayout).setOnPageChangeListener(isA(ExplorePagerScreenListener.class));
    }

    @Test
    public void shouldTrackGenresScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(0);
        TrackingEvent event = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(event.get(ScreenEvent.KEY_SCREEN)).toEqual("explore:genres");
    }

    @Test
    public void shouldTrackTrendingMusicScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(1);
        TrackingEvent event = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(event.get(ScreenEvent.KEY_SCREEN)).toEqual("explore:trending_music");
    }

    @Test
    public void shouldTrackTrendingAudioScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(2);
        TrackingEvent event = eventBus.firstEventOn(EventQueue.TRACKING);
        expect(event.get(ScreenEvent.KEY_SCREEN)).toEqual("explore:trending_audio");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnknownScreenIsViewed() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(3);
    }
}
