package com.soundcloud.android.main;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.main.NavigationFragment.NavItem;
import static com.soundcloud.android.main.NavigationFragment.NavigationCallbacks;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

@RunWith(SoundCloudTestRunner.class)
public class NavigationFragmentTest {

    private NavigationFragment fragment;
    private ImageView avatar;

    @Mock(extraInterfaces = NavigationCallbacks.class) AppCompatActivity activity;
    @Mock Intent intent;
    @Mock SoundCloudApplication application;
    @Mock ViewGroup container;
    @Mock ActionBar actionBar;
    @Mock LayoutInflater layoutInflater;
    @Mock ViewGroup layout;
    @Mock ListView listView;
    @Mock ImageOperations imageOperations;
    @Mock AccountOperations accountOperations;
    @Mock FeatureOperations featureOperations;

    private View upsell;

    @Before
    public void setUp() throws Exception {
        fragment = new NavigationFragment(imageOperations, accountOperations, featureOperations);
        Robolectric.shadowOf(fragment).setActivity(activity);
        when(activity.getIntent()).thenReturn(intent);
        when(activity.getApplication()).thenReturn(application);
        when(activity.getSupportActionBar()).thenReturn(actionBar);
        when(container.getResources()).thenReturn(Robolectric.application.getResources());
        when(layoutInflater.inflate(R.layout.fragment_navigation_drawer, container, false)).thenReturn(layout);
        when(layout.findViewById(R.id.nav_list)).thenReturn(listView);
        fragment.onAttach(activity);

        View navProfileView = LayoutInflater.from(Robolectric.application).inflate(R.layout.nav_profile_item, null, false);
        when(layoutInflater.inflate(R.layout.nav_profile_item, container, false)).thenReturn(navProfileView);
        avatar = (ImageView) navProfileView.findViewById(R.id.avatar);
        PublicApiUser user = ModelFixtures.create(PublicApiUser.class);
        when(accountOperations.getLoggedInUser()).thenReturn(user);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(layoutInflater.inflate(R.layout.nav_profile_item, listView, false)).thenReturn(navProfileView);

        upsell = LayoutInflater.from(Robolectric.application).inflate(R.layout.nav_upsell, null, false);
        when(layout.findViewById(R.id.nav_upsell)).thenReturn(upsell);
        when(featureOperations.shouldShowUpsell()).thenReturn(false);
    }

    @Test
    public void initStateShouldCallbackWithStreamPosition() throws Exception {
        fragment.initState(null);
        verifyPositionSelected(NavItem.STREAM);
    }

    @Test
    public void initStateShouldCallbackWithSavedInstancePosition() {
        Bundle bundle = Mockito.mock(Bundle.class);
        when(bundle.getInt(NavigationFragment.STATE_SELECTED_POSITION)).thenReturn(NavItem.LIKES.ordinal());
        fragment.initState(bundle);
        verifyPositionSelected(NavItem.LIKES);
    }

    @Test
    public void initStateShouldCallbackWithStreamPositionFromAction() {
        when(intent.getAction()).thenReturn(Actions.STREAM);
        fragment.initState(null);
        verifyPositionSelected(NavItem.STREAM);
    }

    @Test
    public void initStateShouldCallbackWithLikesPositionFromAction() {
        when(intent.getAction()).thenReturn(Actions.LIKES);
        fragment.initState(null);
        verifyPositionSelected(NavItem.LIKES);
    }

    @Test
    public void initStateShouldCallbackWithStreamPositionFromUri() {
        when(intent.getData()).thenReturn(Uri.parse("http://souncloud.com/stream/"));
        fragment.initState(null);
        verifyPositionSelected(NavItem.STREAM);
    }

    @Test
    public void initStateShouldCallbackWithStreamPositionFromUriHost() {
        when(intent.getData()).thenReturn(Uri.parse("soundcloud://stream?adj=123"));
        fragment.initState(null);
        verifyPositionSelected(NavItem.STREAM);
    }

    @Test
    public void initStateShouldCallbackWithStreamPositionFromWWWSoundCloudDotCom() {
        when(intent.getData()).thenReturn(Uri.parse("http://www.soundcloud.com"));
        fragment.initState(null);
        verifyPositionSelected(NavItem.STREAM);
    }

    @Test
    public void initStateShouldCallbackWithStreamPositionFromSoundCloudDotCom() {
        when(intent.getData()).thenReturn(Uri.parse("http://soundcloud.com"));
        fragment.initState(null);
        verifyPositionSelected(NavItem.STREAM);
    }

    @Test
    public void initStateShouldCallbackWithExplorePositionFromUri() {
        when(intent.getData()).thenReturn(Uri.parse("http://souncloud.com/explore/"));
        fragment.initState(null);
        verifyPositionSelected(NavItem.EXPLORE);
    }

    @Test
    public void shouldSetCurrentPositionInOnResume() {
        fragment.onCreateView(layoutInflater, container, null);
        fragment.onResume();
        verify(listView).setItemChecked(NavItem.STREAM.ordinal(), true);
    }

    @Test
    public void navListenerShouldCallbackToActivityWhenItemSelected() {
        getOnItemClickListener().onItemClick(listView, null, NavItem.LIKES.ordinal(), 0l);
        verify((NavigationCallbacks) activity).onSmoothSelectItem(NavItem.LIKES.ordinal());
    }

    @Test
    public void shouldSaveSelectedPositionWhenTopLevelItemClicked() {
        getOnItemClickListener().onItemClick(listView, null, NavItem.LIKES.ordinal(), 0l);
        expect(fragment.getCurrentSelectedPosition()).toBe(NavItem.LIKES.ordinal());
    }

    @Test
    public void profileItemIsNotSelectable() {
        getOnItemClickListener().onItemClick(listView, null, NavItem.PROFILE.ordinal(), 0l);
        expect(fragment.getCurrentSelectedPosition()).not.toBe(NavItem.PROFILE.ordinal());
    }

    @Test
    public void shouldNotUpdateProfileAvatarWhenUserIsNotLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        fragment.onCreateView(layoutInflater, container, null);

        verify(imageOperations, never()).displayWithPlaceholder(any(Urn.class), any(ApiImageSize.class), eq(avatar));
    }

    public void upsellIsNotVisibleByDefault() {
        fragment.onCreateView(layoutInflater, container, null);
        fragment.onResume();

        expect(upsell).toBeGone();
    }

    @Test
    public void upsellIsSetVisibleIfEnabled() {
        when(featureOperations.shouldShowUpsell()).thenReturn(true);

        fragment.onCreateView(layoutInflater, container, null);
        fragment.onResume();

        expect(upsell).toBeVisible();
    }

    private void verifyPositionSelected(NavItem navItem) {
        verify((NavigationCallbacks) activity).onSelectItem(navItem.ordinal());
    }

    private AdapterView.OnItemClickListener getOnItemClickListener() {
        fragment.onCreateView(layoutInflater, container, null);
        ArgumentCaptor<AdapterView.OnItemClickListener> listener = ArgumentCaptor.forClass(AdapterView.OnItemClickListener.class);
        verify(listView).setOnItemClickListener(listener.capture());
        return listener.getValue();
    }
}
