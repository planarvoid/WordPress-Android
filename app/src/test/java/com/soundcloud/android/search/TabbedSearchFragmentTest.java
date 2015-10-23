package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;
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

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.View;

public class TabbedSearchFragmentTest extends AndroidUnitTest {

    private TabbedSearchFragment fragment;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private View mockLayout;
    @Mock private TabLayout mockTabLayout;
    @Mock private ViewPager mockViewPager;

    @Before
    public void setUp() throws Exception {
        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.tab_indicator)).thenReturn(mockTabLayout);

        fragment = new TabbedSearchFragment(eventBus, resources());
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents(){
        fragment.setArguments(new Bundle());

        fragment.onViewCreated(mockLayout, null);
        verify(mockViewPager).addOnPageChangeListener(isA(TabbedSearchFragment.SearchPagerScreenListener.class));
    }

    @Test
    public void shouldTrackSearchAllScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(eventBus);
        listener.onPageSelected(0);
        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.get(ScreenEvent.KEY_SCREEN)).isEqualTo("search:everything");
    }

    @Test
    public void shouldTrackSearchTracksScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(eventBus);
        listener.onPageSelected(1);
        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.get(ScreenEvent.KEY_SCREEN)).isEqualTo("search:tracks");
    }

    @Test
    public void shouldTrackSearchPlaylistsScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(eventBus);
        listener.onPageSelected(2);
        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.get(ScreenEvent.KEY_SCREEN)).isEqualTo("search:playlists");
    }

    @Test
    public void shouldTrackSearchPeopleScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(eventBus);
        listener.onPageSelected(3);
        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.get(ScreenEvent.KEY_SCREEN)).isEqualTo("search:people");
    }

}
