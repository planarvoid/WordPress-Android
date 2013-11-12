package com.soundcloud.android.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
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
    ActionBarActivity activity;
    @Mock
    Intent intent;
    @Mock
    SoundCloudApplication application;
    @Mock
    ViewGroup container;
    @Mock
    ActionBar actionBar;
    @Mock
    LayoutInflater layoutInflater;
    @Mock
    ListView listView;

    @Before
    public void setUp() throws Exception {
        fragment = new NavigationFragment();
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setActivity(activity);
        fragment.onAttach(activity);
        when(activity.getIntent()).thenReturn(intent);
        when(activity.getApplication()).thenReturn(application);
        when(activity.getSupportActionBar()).thenReturn(actionBar);
        when(container.getResources()).thenReturn(Robolectric.application.getResources());
        when(layoutInflater.inflate(R.layout.fragment_navigation_listview, container, false)).thenReturn(listView);

        View navProfileView = LayoutInflater.from(Robolectric.application).inflate(R.layout.nav_profile_item, null, false);
        when(layoutInflater.inflate(R.layout.nav_profile_item, container, false)).thenReturn(navProfileView);
    }

    @Test
    public void onCreateShouldCallbackWithStreamPosition() throws Exception {
        fragment.onCreate(null);
        verifyPositionSelected(NavItem.STREAM);
    }

   @Test
    public void onCreateShouldCallbackWithSavedInstancePosition() throws Exception {
       Bundle bundle = Mockito.mock(Bundle.class);
       when(bundle.getInt(NavigationFragment.STATE_SELECTED_POSITION)).thenReturn(NavItem.LIKES.ordinal());
       fragment.onCreate(bundle);
       verifyPositionSelected(NavItem.LIKES);
   }

    @Test
    public void onCreateShouldCallbackWithStreamPositionFromAction() throws Exception {
        when(intent.getAction()).thenReturn(Actions.STREAM);
        fragment.onCreate(null);
        verifyPositionSelected(NavItem.STREAM);
    }

    @Test
    public void onCreateShouldCallbackWithLikesPositionFromAction() throws Exception {
        when(intent.getAction()).thenReturn(Actions.YOUR_LIKES);
        fragment.onCreate(null);
        verifyPositionSelected(NavItem.LIKES);
    }

    @Test
    public void onCreateShouldCallbackWithStreamPositionFromUri() throws Exception {
        when(intent.getData()).thenReturn(Uri.parse("http://souncloud.com/stream/"));
        fragment.onCreate(null);
        verifyPositionSelected(NavItem.STREAM);
    }

    @Test
    public void onCreateShouldCallbackWithExplorePositionFromUri() throws Exception {
        when(intent.getData()).thenReturn(Uri.parse("http://souncloud.com/explore/"));
        fragment.onCreate(null);
        verifyPositionSelected(NavItem.EXPLORE);
    }

    @Test
    public void shouldSetCurrentPositionInOnResume() throws Exception {
        User user = TestHelper.getModelFactory().createModel(User.class);
        when(application.getLoggedInUser()).thenReturn(user);
        fragment.onCreateView(layoutInflater, container, null);
        fragment.onResume();
        verify(listView).setItemChecked(NavItem.STREAM.ordinal(), true);
    }

    private void verifyPositionSelected(NavItem navItem){
        verify((NavigationCallbacks) activity).onNavigationItemSelected(navItem.ordinal(), true);
    }
}
