package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class TabbedSearchFragmentTest {

    private TabbedSearchFragment fragment;
    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private View mockLayout;
    @Mock
    private SlidingTabLayout mockTabIndicator;
    @Mock
    private ViewPager mockViewPager;

    @Before
    public void setUp() throws Exception {
        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.sliding_tabs)).thenReturn(mockTabIndicator);

        fragment = new TabbedSearchFragment(eventBus, Robolectric.application.getResources());
        Robolectric.shadowOf(fragment).setActivity(mock(FragmentActivity.class));
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents(){
        fragment.setArguments(new Bundle());

        fragment.onViewCreated(mockLayout, null);
        verify(mockTabIndicator).setOnPageChangeListener(isA(TabbedSearchFragment.SearchPagerScreenListener.class));
    }

    @Test
    public void shouldTrackSearchAllScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(eventBus);
        listener.onPageSelected(0);
        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.get(ScreenEvent.KEY_SCREEN)).toEqual("search:everything");
    }

    @Test
    public void shouldTrackSearchTracksScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(eventBus);
        listener.onPageSelected(1);
        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.get(ScreenEvent.KEY_SCREEN)).toEqual("search:tracks");
    }

    @Test
    public void shouldTrackSearchPlaylistsScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(eventBus);
        listener.onPageSelected(2);
        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.get(ScreenEvent.KEY_SCREEN)).toEqual("search:playlists");
    }

    @Test
    public void shouldTrackSearchPeopleScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(eventBus);
        listener.onPageSelected(3);
        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.get(ScreenEvent.KEY_SCREEN)).toEqual("search:people");
    }
}
