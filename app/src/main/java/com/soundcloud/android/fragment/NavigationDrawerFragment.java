package com.soundcloud.android.fragment;

import com.github.espiandev.showcaseview.ShowcaseView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.showcases.Showcase;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.images.ImageOptionsFactory;
import com.soundcloud.android.utils.images.ImageSize;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.EnumSet;

public class NavigationDrawerFragment extends Fragment {

    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    private NavigationDrawerCallbacks mCallbacks;
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 1; //Default to Stream
    private ShowcaseView mCurrentMenuShowcase;

    private ProfileViewHolder mProfileViewHolder;

    public enum NavItem {
        PROFILE(R.string.side_menu_profile),
        STREAM(R.string.side_menu_stream),
        EXPLORE(R.string.side_menu_explore),
        LIKES(R.string.side_menu_likes),
        PLAYLISTS(R.string.side_menu_playlists);

        private final int textId;
        private NavItem(int textId){
            this.textId = textId;
        }
    }

    // normal rows (below profile)
    private static final EnumSet<NavItem> TEXT_NAV_ITEMS =
            EnumSet.of(NavItem.STREAM, NavItem.EXPLORE, NavItem.LIKES, NavItem.PLAYLISTS);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            selectItem(savedInstanceState.getInt(STATE_SELECTED_POSITION));

        } else if (!handleIntent(getActivity().getIntent())){
            selectItem(mCurrentSelectedPosition);
        }
    }

    public boolean handleIntent(Intent intent){
        final String action = intent.getAction();
        if (ScTextUtils.isNotBlank(action)){
            if (Actions.STREAM.equals(action)){
                selectItem(NavItem.STREAM.ordinal());
                return true;

            } else if (Actions.YOUR_LIKES.equals(action)){
                selectItem(NavItem.LIKES.ordinal());
                return true;
            }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate( R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });


        final View view = inflater.inflate(R.layout.nav_drawer_profile_item, container, false);
        mDrawerListView.addHeaderView(view);

        mProfileViewHolder = new ProfileViewHolder();
        mProfileViewHolder.imageView = (ImageView) view.findViewById(R.id.avatar);
        mProfileViewHolder.username = (TextView) view.findViewById(R.id.username);
        mProfileViewHolder.location = (TextView) view.findViewById(R.id.location);

        configureProfileItem(((SoundCloudApplication) getActivity().getApplication()).getLoggedInUser());

        int i = 0;
        String[] rows = new String[TEXT_NAV_ITEMS.size()];
        for (NavItem textItem : TEXT_NAV_ITEMS){
            rows[i++] = getString(textItem.textId);
        }

        mDrawerListView.setAdapter(new ArrayAdapter<String>(
                getActionBar().getThemedContext(),
                R.layout.nav_drawer_item,
                R.id.nav_item_text,
                rows));

        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        return mDrawerListView;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
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

                if (mCurrentMenuShowcase != null && mCurrentMenuShowcase.isShown()){
                    mCurrentMenuShowcase.hide();
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()

                mCurrentMenuShowcase = Showcase.EXPLORE.insertShowcase(getActivity(),
                        mDrawerListView.getChildAt(2).findViewById(R.id.nav_item_text));
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public void updateProfileItem(User user) {
        configureProfileItem(user);
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int position);
    }

    private void configureProfileItem(User user) {
        mProfileViewHolder.username.setText(user.getUsername());
        final String location = user.getLocation();
        if (ScTextUtils.isNotBlank(location)) {
            mProfileViewHolder.location.setText(location);
        } else {
            mProfileViewHolder.location.setVisibility(View.GONE);
        }

        ImageLoader.getInstance().displayImage(ImageSize.T500.formatUri(user.getNonDefaultAvatarUrl()),
                mProfileViewHolder.imageView, ImageOptionsFactory.adapterView(R.drawable.placeholder_cells));
    }

    private static class ProfileViewHolder {
        public ImageView imageView;
        public TextView username, location;
    }
}
