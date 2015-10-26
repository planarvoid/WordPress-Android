package com.soundcloud.android.explore;

import static com.soundcloud.android.explore.ExploreFragment.ExplorePagerScreenListener;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
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

    private ExploreFragment fragment;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.tab_indicator)).thenReturn(mockTabLayout);
        when(adapterFactory.create(any(FragmentManager.class))).thenReturn(pagerAdapter);

        fragment = new ExploreFragment(resources(), adapterFactory, eventBus);

        fragment.onCreate(null);
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents() {
        when(mockLayout.findViewById(R.id.tab_indicator)).thenReturn(mockTabLayout);
        fragment.onViewCreated(mockLayout, null);
        verify(mockViewPager).addOnPageChangeListener(isA(ExplorePagerScreenListener.class));
    }

    @Test
    public void shouldTrackGenresScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(0);
        TrackingEvent event = eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(event.get(ScreenEvent.KEY_SCREEN)).isEqualTo("explore:genres");
    }

    @Test
    public void shouldTrackTrendingMusicScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(1);
        TrackingEvent event = eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(event.get(ScreenEvent.KEY_SCREEN)).isEqualTo("explore:trending_music");
    }

    @Test
    public void shouldTrackTrendingAudioScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(2);
        TrackingEvent event = eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(event.get(ScreenEvent.KEY_SCREEN)).isEqualTo("explore:trending_audio");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnknownScreenIsViewed() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(3);
    }

}
