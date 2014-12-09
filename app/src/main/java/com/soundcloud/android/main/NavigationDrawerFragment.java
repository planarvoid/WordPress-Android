package com.soundcloud.android.main;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

// This guy needs a thorough refactor. We should pull out all the drawer presentation logic into a testable object,
// since it needs to deal with awkward life cycle stuff where the drawer layout can be null in many cases
@SuppressLint("ValidFragment")
public class NavigationDrawerFragment extends NavigationFragment {

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;

    private Subscription subscription = Subscriptions.empty();

    @Inject EventBus eventBus;

    public NavigationDrawerFragment() {
        // Android needs a default ctor.
    }

    @VisibleForTesting
    protected NavigationDrawerFragment(ImageOperations imageOperations, AccountOperations accountOperations, EventBus eventBus) {
        super(imageOperations, accountOperations);
        this.eventBus = eventBus;
    }

    // View initialization needs to be here since the fragment relies on views owned by the activity. We should just
    // rewrite this guy to not be a fragment, as this is really backwards.
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        drawerLayout = setupDrawerLayout();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscription = eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerExpansionSubscriber());
    }

    private DrawerLayout setupDrawerLayout() {
        DrawerLayout drawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.shadow_right, GravityCompat.START);
        setupDrawerToggle(drawerLayout);
        return drawerLayout;
    }

    private void setupDrawerToggle(final DrawerLayout drawerLayout) {
        drawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                drawerLayout,                    /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAttached()) {
                    return;
                }
                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAttached()) {
                    return;
                }
                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SIDE_MENU_DRAWER));
            }
        };

        // Defer code dependent on restoration of previous instance state.
        drawerLayout.post(new Runnable() {
            @Override
            public void run() {
                drawerToggle.syncState();
            }
        });
        drawerLayout.setDrawerListener(drawerToggle);
    }

    @Override
    public void onDestroyView() {
        subscription.unsubscribe();
        super.onDestroyView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        if (isAttached()) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isAttached()) {
            if (drawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
            closeDrawer();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (isDrawerOpen()) {
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    protected boolean shouldSetActionBarTitle() {
        return !isDrawerOpen();
    }

    public boolean isDrawerOpen() {
        return isAttached() && drawerLayout.isDrawerOpen(getView());
    }

    public void closeDrawer() {
        if (isDrawerOpen()) {
            drawerLayout.closeDrawer(getView());
        }
    }

    @Override
    protected void smoothSelectItem(int position) {
        super.smoothSelectItem(position);
        closeDrawer();
    }

    @Override
    protected void selectItem(int position) {
        super.selectItem(position);
        closeDrawer();
    }

    @Override
    protected void configureLocalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setTitle(R.string.app_name);
    }

    private boolean isAttached() {
        return isAdded() && drawerLayout != null;
    }

    private final class PlayerExpansionSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (isAttached()) {
                if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                } else if (event.getKind() == PlayerUIEvent.PLAYER_COLLAPSED) {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                }
            }
        }
    }

}
