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
import android.content.Context;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.EnumSet;

public class NavigationDrawerFragment extends Fragment {

    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static final int NO_IMAGE = -1;

    private NavigationDrawerCallbacks mCallbacks;
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;

    private int mCurrentSelectedPosition = NavItem.STREAM.ordinal();
    private ShowcaseView mCurrentMenuShowcase;

    private ProfileViewHolder mProfileViewHolder;

    public enum NavItem {
        PROFILE(R.string.side_menu_profile, NO_IMAGE),
        STREAM(R.string.side_menu_stream, R.drawable.drawer_stream_states),
        EXPLORE(R.string.side_menu_explore, R.drawable.drawer_explore_states),
        LIKES(R.string.side_menu_likes, R.drawable.drawer_likes_states),
        PLAYLISTS(R.string.side_menu_playlists, R.drawable.drawer_playlists_states);

        private final int textId;
        private final int imageId;

        private NavItem(int textId, int imageId) {
            this.textId = textId;
            this.imageId = imageId;
        }
    }

    // normal rows (below profile)
    private static final EnumSet<NavItem> TEXT_NAV_ITEMS =
            EnumSet.of(NavItem.STREAM, NavItem.EXPLORE, NavItem.LIKES, NavItem.PLAYLISTS);


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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            final int previousPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            // TODO: user profile needs special treatment since it's an Activity, not a fragment
            // Can remove this once we port this over to be a fragment as well
            if (previousPosition != NavItem.PROFILE.ordinal()) {
                selectItem(previousPosition);
            }

        } else if (!handleIntent(getActivity().getIntent())) {
            selectItem(mCurrentSelectedPosition);
        }
    }

    public boolean handleIntent(Intent intent) {
        final String action = intent.getAction();
        if (ScTextUtils.isNotBlank(action)) {
            if (Actions.STREAM.equals(action)) {
                selectItem(NavItem.STREAM.ordinal());
                return true;

            } else if (Actions.YOUR_LIKES.equals(action)) {
                selectItem(NavItem.LIKES.ordinal());
                return true;
            }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerListView = setupDrawerListView(inflater, container);
        return mDrawerListView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDrawerLayout = setupDrawerLayout();
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
        if (isDrawerOpen()) {
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

    private ListView setupDrawerListView(LayoutInflater inflater, ViewGroup container) {
        ListView drawerListView = (ListView) inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        drawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        drawerListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        View userProfileHeader = setupUserProfileHeader(inflater, container);
        drawerListView.addHeaderView(userProfileHeader);

        int i = 0;
        NavItem[] data = new NavItem[TEXT_NAV_ITEMS.size()];
        for (NavItem navItem : TEXT_NAV_ITEMS) {
            data[i++] = navItem;
        }

        drawerListView.setAdapter(new DrawerAdapter(
                getActionBar().getThemedContext(),
                R.layout.nav_drawer_item, data));

        drawerListView.setItemChecked(mCurrentSelectedPosition, true);

        return drawerListView;
    }

    private View setupUserProfileHeader(LayoutInflater inflater, ViewGroup container) {
        final View view = inflater.inflate(R.layout.nav_drawer_profile_item, container, false);

        mProfileViewHolder = new ProfileViewHolder();
        mProfileViewHolder.imageView = (ImageView) view.findViewById(R.id.avatar);
        mProfileViewHolder.username = (TextView) view.findViewById(R.id.username);
        mProfileViewHolder.followers = (TextView) view.findViewById(R.id.followers_count);

        updateProfileItem(((SoundCloudApplication) getActivity().getApplication()).getLoggedInUser());

        return view;
    }

    private DrawerLayout setupDrawerLayout() {
        DrawerLayout drawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
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

                if (mCurrentMenuShowcase != null && mCurrentMenuShowcase.isShown()) {
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
        drawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        drawerLayout.setDrawerListener(mDrawerToggle);

        return drawerLayout;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(getView());
    }

    public void closeDrawer() {
        if (isDrawerOpen()) {
            mDrawerLayout.closeDrawer(getView());
        }
    }

    public void updateProfileItem(User user) {
        mProfileViewHolder.username.setText(user.getUsername());
        mProfileViewHolder.followers.setText(getResources().getQuantityString(
                R.plurals.number_of_followers, user.followers_count, user.followers_count));

        ImageLoader.getInstance().displayImage(ImageSize.T500.formatUri(user.getNonDefaultAvatarUrl()),
                mProfileViewHolder.imageView, ImageOptionsFactory.adapterView(R.drawable.placeholder_cells));
    }

    private void selectItem(int position) {
        // TODO: Since the user profile currently opens in a new activity, we must not adjust the current selection
        // index. Remove this workaround when the user browser has become a fragment too
        if (mDrawerListView != null) {
            int checkedPosition = position == NavItem.PROFILE.ordinal() ? mCurrentSelectedPosition : position;
            mDrawerListView.setItemChecked(checkedPosition, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(getView());
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
        mCurrentSelectedPosition = position;
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

    private static class ProfileViewHolder {
        public ImageView imageView;
        public TextView username, followers;
    }

    private class DrawerAdapter extends ArrayAdapter<NavItem> {

        public DrawerAdapter(Context context, int layoutResourceId, NavItem[] data) {
            super(context, layoutResourceId, data);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            NavItem navItem = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.nav_drawer_item, parent, false);
                holder = new ViewHolder();
                holder.icon = (ImageView) convertView.findViewById(R.id.nav_item_image);
                holder.text = (TextView) convertView.findViewById(R.id.nav_item_text);
                convertView.setTag(holder);
            } else
                holder = (ViewHolder) convertView.getTag();

            holder.text.setText(navItem.textId);
            holder.icon.setImageResource(navItem.imageId);
            return convertView;
        }

        private class ViewHolder {
            ImageView icon;
            TextView text;
        }


    }
}
