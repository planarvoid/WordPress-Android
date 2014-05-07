package com.soundcloud.android.main;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.main.NavigationFragment.NavItem;
import static com.soundcloud.android.main.NavigationFragment.NavigationCallbacks;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

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
    @Mock
    ImageOperations imageOperations;
    @Mock
    AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        fragment = new NavigationFragment(imageOperations, accountOperations);
        Robolectric.shadowOf(fragment).setActivity(activity);
        when(activity.getIntent()).thenReturn(intent);
        when(activity.getApplication()).thenReturn(application);
        when(activity.getSupportActionBar()).thenReturn(actionBar);
        when(container.getResources()).thenReturn(Robolectric.application.getResources());
        when(layoutInflater.inflate(R.layout.fragment_navigation_listview, container, false)).thenReturn(listView);
        fragment.onAttach(activity);

        View navProfileView = LayoutInflater.from(Robolectric.application).inflate(R.layout.nav_profile_item, null, false);
        when(layoutInflater.inflate(R.layout.nav_profile_item, container, false)).thenReturn(navProfileView);
        User user = TestHelper.getModelFactory().createModel(User.class);
        when(accountOperations.getLoggedInUser()).thenReturn(user);
    }

    @Test
    public void initStateShouldCallbackWithStreamPosition() throws Exception {
        fragment.initState(null);
        verifyPositionSelected(NavItem.STREAM);
    }

   @Test
    public void initStateShouldCallbackWithSavedInstancePosition() throws Exception {
       Bundle bundle = Mockito.mock(Bundle.class);
       when(bundle.getInt(NavigationFragment.STATE_SELECTED_POSITION)).thenReturn(NavItem.LIKES.ordinal());
       fragment.initState(bundle);
       verifyPositionSelected(NavItem.LIKES);
   }

    @Test
    public void initStateShouldCallbackWithStreamPositionFromAction() throws Exception {
        when(intent.getAction()).thenReturn(Actions.STREAM);
        fragment.initState(null);
        verifyPositionSelected(NavItem.STREAM);
    }

    @Test
    public void initStateShouldCallbackWithLikesPositionFromAction() throws Exception {
        when(intent.getAction()).thenReturn(Actions.LIKES);
        fragment.initState(null);
        verifyPositionSelected(NavItem.LIKES);
    }

    @Test
    public void initStateShouldCallbackWithStreamPositionFromUri() throws Exception {
        when(intent.getData()).thenReturn(Uri.parse("http://souncloud.com/stream/"));
        fragment.initState(null);
        verifyPositionSelected(NavItem.STREAM);
    }

    @Test
    public void initStateShouldCallbackWithExplorePositionFromUri() throws Exception {
        when(intent.getData()).thenReturn(Uri.parse("http://souncloud.com/explore/"));
        fragment.initState(null);
        verifyPositionSelected(NavItem.EXPLORE);
    }

    @Test
    public void shouldSetCurrentPositionInOnResume() throws Exception {
        fragment.onCreateView(layoutInflater, container, null);
        fragment.onResume();
        verify(listView).setItemChecked(NavItem.STREAM.ordinal(), true);
    }

    @Test
    public void navListenerShouldCallbackToActivityWhenItemSelected() throws Exception {
        getOnItemClickListener().onItemClick(listView, null, NavItem.LIKES.ordinal(), 0l);
        verifyPositionSelected(NavItem.LIKES);
    }

    @Test
    public void shouldSaveSelectedPositionWhenTopLevelItemClicked() throws Exception {
        getOnItemClickListener().onItemClick(listView, null, NavItem.LIKES.ordinal(), 0l);
        expect(fragment.getCurrentSelectedPosition()).toBe(NavItem.LIKES.ordinal());
    }

    @Test
    public void shouldNotSaveSelectedPositionWhenProfileClicked() throws Exception {
        getOnItemClickListener().onItemClick(listView, null, NavItem.PROFILE.ordinal(), 0l);
        expect(fragment.getCurrentSelectedPosition()).not.toBe(NavItem.PROFILE.ordinal());
    }

    private void verifyPositionSelected(NavItem navItem){
        verify((NavigationCallbacks) activity).onNavigationItemSelected(navItem.ordinal(), true);
    }

    private AdapterView.OnItemClickListener getOnItemClickListener() {
        fragment.onCreateView(layoutInflater, container, null);
        ArgumentCaptor<AdapterView.OnItemClickListener> listener = ArgumentCaptor.forClass(AdapterView.OnItemClickListener.class);
        verify(listView).setOnItemClickListener(listener.capture());
        return listener.getValue();
    }
}
