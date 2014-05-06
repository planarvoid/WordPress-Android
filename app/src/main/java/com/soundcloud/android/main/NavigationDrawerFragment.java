package com.soundcloud.android.main;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import org.jetbrains.annotations.Nullable;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class NavigationDrawerFragment extends NavigationFragment {

    @Nullable
    private ActionBarDrawerToggle drawerToggle;

    private DrawerLayout drawerLayout;

    @Inject
    EventBus eventBus;

    public NavigationDrawerFragment() {
    }

    @VisibleForTesting
    protected NavigationDrawerFragment(ImageOperations imageOperations, EventBus eventBus) {
        super(imageOperations);
        this.eventBus = eventBus;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        drawerLayout = setupDrawerLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        closeDrawer();
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
        final View view = getView();
        return drawerLayout != null && view != null && drawerLayout.isDrawerOpen(view);
    }

    public void closeDrawer() {
        if (isDrawerOpen()) {
            drawerLayout.closeDrawer(getView());
        }
    }

    public void setLocked(boolean locked) {
        drawerLayout.setDrawerLockMode(locked ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    protected void selectItem(int position) {
        super.selectItem(position);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(getView());
        }
    }

    @Override
    protected void configureLocalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    private DrawerLayout setupDrawerLayout() {
        DrawerLayout drawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.shadow_right, GravityCompat.START);
        setupDrawerToggle(drawerLayout);
        return drawerLayout;
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

    private void setupDrawerToggle(final DrawerLayout drawerLayout) {
        drawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                drawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }
                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }
                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
                eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SIDE_MENU_DRAWER.get());
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
}
