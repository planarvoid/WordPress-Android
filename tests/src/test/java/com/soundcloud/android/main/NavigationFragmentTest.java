package com.soundcloud.android.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.soundcloud.android.Actions;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import static com.soundcloud.android.main.NavigationFragment.NavItem;
import static com.soundcloud.android.main.NavigationFragment.NavigationCallbacks;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SoundCloudTestRunner.class)
public class NavigationFragmentTest {

    NavigationFragment fragment;

    @Mock(extraInterfaces = NavigationCallbacks.class)
    FragmentActivity activity;
    @Mock
    Intent intent;

    @Before
    public void setUp() throws Exception {
        fragment = new NavigationFragment();
        Robolectric.shadowOf(fragment).setActivity(activity);
        fragment.onAttach(activity);
        when(activity.getIntent()).thenReturn(intent);
    }

    @Test
    public void onCreateShouldCallbackWithStreamPositionAndSetTitleAsTrue() throws Exception {
        fragment.onCreate(null);
        verify((NavigationCallbacks) activity).onNavigationItemSelected(NavItem.STREAM.ordinal(), true);
    }

   @Test
    public void onCreateShouldCallbackWithSavedInstancePosition() throws Exception {
       Bundle bundle = Mockito.mock(Bundle.class);
       when(bundle.getInt(NavigationFragment.STATE_SELECTED_POSITION)).thenReturn(NavItem.LIKES.ordinal());
       fragment.onCreate(bundle);
       verify((NavigationCallbacks) activity).onNavigationItemSelected(NavItem.LIKES.ordinal(), true);
   }

    @Test
    public void onCreateShouldCallbackWithStreamPositionFromAction() throws Exception {
        when(intent.getAction()).thenReturn(Actions.STREAM);
        fragment.onCreate(null);
        verify((NavigationCallbacks) activity).onNavigationItemSelected(NavItem.STREAM.ordinal(), true);
    }

    @Test
    public void onCreateShouldCallbackWithLikesPositionFromAction() throws Exception {
        when(intent.getAction()).thenReturn(Actions.YOUR_LIKES);
        fragment.onCreate(null);
        verify((NavigationCallbacks) activity).onNavigationItemSelected(NavItem.LIKES.ordinal(), true);
    }

    @Test
    public void onCreateShouldCallbackWithStreamPositionFromUri() throws Exception {
        when(intent.getData()).thenReturn(Uri.parse("http://souncloud.com/stream/"));
        fragment.onCreate(null);
        verify((NavigationCallbacks) activity).onNavigationItemSelected(NavItem.STREAM.ordinal(), true);
    }

    @Test
    public void onCreateShouldCallbackWithExplorePositionFromUri() throws Exception {
        when(intent.getData()).thenReturn(Uri.parse("http://souncloud.com/explore/"));
        fragment.onCreate(null);
        verify((NavigationCallbacks) activity).onNavigationItemSelected(NavItem.EXPLORE.ordinal(), true);
    }

}
