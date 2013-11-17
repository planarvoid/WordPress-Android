package com.soundcloud.android.main;

import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SoundCloudTestRunner.class)
public class NavigationDrawerFragmentTest {

    private NavigationDrawerFragment fragment;

    @Mock(extraInterfaces = NavigationFragment.NavigationCallbacks.class)
    ActionBarActivity activity;
    @Mock
    DrawerLayout drawerLayout;
    @Mock
    ActionBar actionBar;
    @Mock
    View view;

    @Before
    public void setUp() throws Exception {
        fragment = new NavigationDrawerFragment();
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setView(view);
        when(activity.findViewById(R.id.drawer_layout)).thenReturn(drawerLayout);
        when(activity.getSupportActionBar()).thenReturn(actionBar);
        fragment.onAttach(activity);
        fragment.onActivityCreated(null);
    }

    @Test
    public void shouldNotTryToCloseDrawerIfCloseIsCalledAndNotOpen() throws Exception {
        fragment.closeDrawer();
        verify(drawerLayout, never()).closeDrawer(any(View.class));
    }

    @Test
    public void shouldTryToCloseDrawerIfCloseIsCalledAndOpen() throws Exception {
        when(drawerLayout.isDrawerOpen(view)).thenReturn(true);
        fragment.closeDrawer();
        verify(drawerLayout).closeDrawer(view);
    }

    @Test
    public void shouldSetupGlobalContextActionBarWhenDrawerOpened() throws Exception {
        when(drawerLayout.isDrawerOpen(view)).thenReturn(true);
        fragment.onCreateOptionsMenu(Mockito.mock(Menu.class), Mockito.mock(MenuInflater.class));
        verify(actionBar).setDisplayShowCustomEnabled(false);
        verify(actionBar).setTitle(R.string.app_name);
    }

}
