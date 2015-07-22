package com.soundcloud.android.main;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class NavigationDrawerFragmentTest {

    private NavigationDrawerFragment fragment;

    @Mock(extraInterfaces = NavigationFragment.NavigationCallbacks.class) AppCompatActivity activity;
    @Mock DrawerLayout drawerLayout;
    @Mock ActionBar actionBar;
    @Mock View view;
    @Mock ImageOperations imageOperations;
    @Mock AccountOperations accountOperations;
    @Mock Resources resources;
    @Mock FeatureOperations featureOperations;
    @Mock FeatureFlags featureFlags;

    TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        fragment = new NavigationDrawerFragment(imageOperations, accountOperations, featureOperations, eventBus, featureFlags);
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setView(view);
        Robolectric.shadowOf(fragment).setAttached(true);
        when(activity.findViewById(R.id.drawer_layout)).thenReturn(drawerLayout);
        when(activity.getSupportActionBar()).thenReturn(actionBar);

        SoundCloudApplication application = mock(SoundCloudApplication.class);
        when(application.getEventBus()).thenReturn(eventBus);
        when(activity.getApplication()).thenReturn(application);

        fragment.onAttach(activity);
    }

    @Test
    public void shouldNotTryToCloseDrawerIfCloseIsCalledAndNotOpen() throws Exception {
        fragment.closeDrawer();
        verify(drawerLayout, never()).closeDrawer(any(View.class));
    }

    @Test
    public void shouldTryToCloseDrawerIfCloseIsCalledAndOpen() throws Exception {
        createActivityWithOpenedDrawer();

        fragment.closeDrawer();
        verify(drawerLayout).closeDrawer(view);
    }

    @Test
    public void handleBackPressedClosesDrawerWhenOpened() {
        createActivityWithOpenedDrawer();

        boolean drawerClosed = fragment.handleBackPressed();

        expect(drawerClosed).toBeTrue();
        verify(drawerLayout).closeDrawer(view);
    }

    @Test
    public void handleBackPressedDoesNothingWhenDrawerClosed() {
        expect(fragment.handleBackPressed()).toBeFalse();

        verify(drawerLayout, never()).closeDrawer(view);
    }

    @Test
    public void shouldLockDrawerOnPlayerExpandedEvent() {
        fragment.onViewCreated(view, null);
        fragment.onActivityCreated(null);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Test
    public void shouldUnlockDrawerOnPlayerCollapsedEvent() {
        fragment.onViewCreated(view, null);
        fragment.onActivityCreated(null);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Test
    public void shouldIgnoreDrawerEventsUntilDrawerLayoutCompleted() {
        fragment.onViewCreated(view, null);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verifyZeroInteractions(drawerLayout);
    }

    @Test
    public void shouldUnsubscribeFromUIEventsInOnDestroyView() {
        fragment.onViewCreated(view, null);
        fragment.onDestroyView();
        eventBus.verifyUnsubscribed();
    }

    private void createActivityWithOpenedDrawer() {
        fragment.onActivityCreated(null);
        when(drawerLayout.isDrawerOpen(view)).thenReturn(true);
    }

}
