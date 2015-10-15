package com.soundcloud.android.main;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;

// This guy needs a thorough refactor. We should pull out all the drawer presentation logic into a testable object,
// since it needs to deal with awkward life cycle stuff where the drawer layout can be null in many cases
@SuppressLint("ValidFragment")
public class LegacyNavigationDrawerFragment extends LegacyNavigationFragment {

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;

    private Subscription subscription = RxUtils.invalidSubscription();


    public LegacyNavigationDrawerFragment() {
        // Android needs a default constructor.
    }

    @VisibleForTesting
    protected LegacyNavigationDrawerFragment(ImageOperations imageOperations, AccountOperations accountOperations,
                                             FeatureOperations featureOperations, FeatureFlags featureFlags, EventBus eventBus) {
        super(imageOperations, accountOperations, featureOperations, featureFlags, eventBus);
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

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (drawerView != null) { // this removes the drawer icon open-close animation
                    super.onDrawerSlide(drawerView, 0);
                }
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

    public boolean isDrawerOpen() {
        return isAttached() && drawerLayout.isDrawerOpen(getView());
    }

    public void closeDrawer() {
        if (isDrawerOpen()) {
            drawerLayout.closeDrawer(getView());
        }
    }

    @Override
    protected void smoothSelectItem(int position, NavItem item) {
        super.smoothSelectItem(position, item);
        closeDrawer();
    }

    @Override
    protected void configureLocalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    @Override
    public boolean handleBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer();
            return true;
        }
        return false;
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
