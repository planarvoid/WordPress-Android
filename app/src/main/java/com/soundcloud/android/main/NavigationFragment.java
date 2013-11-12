package com.soundcloud.android.main;

import com.github.espiandev.showcaseview.ShowcaseView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.images.ImageOptionsFactory;
import com.soundcloud.android.utils.images.ImageSize;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.EnumSet;

public class NavigationFragment extends Fragment {

    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static final int NO_IMAGE = -1;

    private NavigationDrawerCallbacks mCallbacks;

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

        if (intent.getData() != null) {
            if (intent.getData().getLastPathSegment().equals("stream")) {
                selectItem(NavItem.STREAM.ordinal());
                return true;
            } else if (intent.getData().getLastPathSegment().equals("explore")) {
                selectItem(NavItem.EXPLORE.ordinal());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update the checked state of the nav items to the last known position. It's important to do this in onResume
        // as long as the user profile opens in a new activity, since when returning via the up button would otherwise
        // not update it to the last selected content fragment
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerListView = setupDrawerListView(inflater, container);
        return mDrawerListView;
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

    protected void openShowcase() {
        mCurrentMenuShowcase = Showcase.EXPLORE.insertShowcase(getActivity(),
                mDrawerListView.getChildAt(2).findViewById(R.id.nav_item_text));
    }

    protected void closeShowcase() {
        if (mCurrentMenuShowcase != null && mCurrentMenuShowcase.isShown()) {
            mCurrentMenuShowcase.hide();
        }
    }

    public void updateProfileItem(User user) {
        mProfileViewHolder.username.setText(user.getUsername());
        int followersCount = user.followers_count < 0 ? 0 : user.followers_count;
        mProfileViewHolder.followers.setText(getResources().getQuantityString(
                R.plurals.number_of_followers, followersCount, followersCount));

        String imageUri = ImageSize.formatUriForFullDisplay(getResources(), user.getNonDefaultAvatarUrl());
        ImageLoader.getInstance().displayImage(imageUri, mProfileViewHolder.imageView,
                ImageOptionsFactory.adapterView(R.drawable.placeholder_cells));
    }

    protected void selectItem(int position) {
        // TODO: Since the user profile currently opens in a new activity, we must not adjust the current selection
        // index. Remove this workaround when the user browser has become a fragment too
        if (position != NavItem.PROFILE.ordinal()) {
            mCurrentSelectedPosition = position;
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position, shouldSetActionBarTitle());
        }
    }

    protected boolean shouldSetActionBarTitle() {
        return true;
    }

    protected ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int position, boolean setTitle);
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
