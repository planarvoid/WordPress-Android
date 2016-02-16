package com.soundcloud.android.search;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.discovery.SearchTracker;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.View;

public class TabbedSearchFragmentTest extends AndroidUnitTest {

    private TabbedSearchFragment fragment;

    @Mock private View mockLayout;
    @Mock private TabLayout mockTabLayout;
    @Mock private ViewPager mockViewPager;
    @Mock private SearchTracker searchTracker;

    @Before
    public void setUp() throws Exception {
        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.tab_indicator)).thenReturn(mockTabLayout);

        fragment = new TabbedSearchFragment(resources(), searchTracker);
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents() {
        fragment.setArguments(new Bundle());

        fragment.onViewCreated(mockLayout, null);
        verify(mockViewPager).addOnPageChangeListener(isA(TabbedSearchFragment.SearchPagerScreenListener.class));
    }

    @Test
    public void shouldTrackSearchAllScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(searchTracker);
        listener.onPageSelected(0);
        verify(searchTracker).trackResultsScreenEvent(Screen.SEARCH_EVERYTHING);
    }

    @Test
    public void shouldTrackSearchTracksScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(searchTracker);
        listener.onPageSelected(1);
        verify(searchTracker).trackResultsScreenEvent(Screen.SEARCH_TRACKS);
    }

    @Test
    public void shouldTrackSearchPlaylistsScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(searchTracker);
        listener.onPageSelected(2);
        verify(searchTracker).trackResultsScreenEvent(Screen.SEARCH_PLAYLISTS);
    }

    @Test
    public void shouldTrackSearchPeopleScreenOnPageSelected() throws Exception {
        TabbedSearchFragment.SearchPagerScreenListener listener = new TabbedSearchFragment.SearchPagerScreenListener(searchTracker);
        listener.onPageSelected(3);
        verify(searchTracker).trackResultsScreenEvent(Screen.SEARCH_USERS);
    }

}
