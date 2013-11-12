package com.soundcloud.android.main;

import android.app.Application;
import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;
import com.github.espiandev.showcaseview.ShowcaseView;
import com.google.common.annotations.VisibleForTesting;
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

    @VisibleForTesting
    static final String STATE_SELECTED_POSITION = "selected_navigation_position";
    private static final int NO_IMAGE = -1;

    private NavigationCallbacks mCallbacks;

    private ListView mListView;

    private int mCurrentSelectedPosition = NavItem.STREAM.ordinal();
    private ShowcaseView mCurrentMenuShowcase;

    private ProfileViewHolder mProfileViewHolder;

    public enum NavItem {
        PROFILE(R.string.side_menu_profile, NO_IMAGE),
        STREAM(R.string.side_menu_stream, R.drawable.nav_stream_states),
        EXPLORE(R.string.side_menu_explore, R.drawable.nav_explore_states),
        LIKES(R.string.side_menu_likes, R.drawable.nav_likes_states),
        PLAYLISTS(R.string.side_menu_playlists, R.drawable.nav_playlists_states);

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
            mCallbacks = (NavigationCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationCallbacks.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            selectItem(savedInstanceState.getInt(STATE_SELECTED_POSITION));
        } else if (!handleIntent(getActivity().getIntent())) {
            selectItem(mCurrentSelectedPosition);
        }
    }

    @VisibleForTesting
    int getCurrentSelectedPosition() {
        return mCurrentSelectedPosition;
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
        mListView.setItemChecked(mCurrentSelectedPosition, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mListView = setupListView(inflater, container);
        return mListView;
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

    private ListView setupListView(LayoutInflater inflater, ViewGroup container) {
        ListView listView = (ListView) inflater.inflate(R.layout.fragment_navigation_listview, container, false);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        View userProfileHeader = setupUserProfileHeader(inflater, container);
        listView.addHeaderView(userProfileHeader);

        int i = 0;
        NavItem[] data = new NavItem[TEXT_NAV_ITEMS.size()];
        for (NavItem navItem : TEXT_NAV_ITEMS) {
            data[i++] = navItem;
        }

        listView.setAdapter(new NavigationAdapter(
                getActionBar().getThemedContext(),
                R.layout.nav_item, data));

        return listView;
    }

    private View setupUserProfileHeader(LayoutInflater inflater, ViewGroup container) {
        final View view = inflater.inflate(R.layout.nav_profile_item, container, false);

        mProfileViewHolder = new ProfileViewHolder();
        mProfileViewHolder.imageView = (ImageView) view.findViewById(R.id.avatar);
        mProfileViewHolder.username = (TextView) view.findViewById(R.id.username);
        mProfileViewHolder.followers = (TextView) view.findViewById(R.id.followers_count);

        updateProfileItem(((SoundCloudApplication) getActivity().getApplication()).getLoggedInUser(), container.getResources());

        return view;
    }

    protected void openShowcase() {
        mCurrentMenuShowcase = Showcase.EXPLORE.insertShowcase(getActivity(),
                mListView.getChildAt(2).findViewById(R.id.nav_item_text));
    }

    protected void closeShowcase() {
        if (mCurrentMenuShowcase != null && mCurrentMenuShowcase.isShown()) {
            mCurrentMenuShowcase.hide();
        }
    }

    // XXX we are passing in resources to overcome Robolectric 1 null getResources in Fragments
    // Can be removed once we migrate to RL2
    public void updateProfileItem(User user, Resources resources) {
        mProfileViewHolder.username.setText(user.getUsername());
        int followersCount = user.followers_count < 0 ? 0 : user.followers_count;
        mProfileViewHolder.followers.setText(resources.getQuantityString(
                R.plurals.number_of_followers, followersCount, followersCount));

        String imageUri = ImageSize.formatUriForFullDisplay(resources, user.getNonDefaultAvatarUrl());
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
            mCallbacks.onNavigationItemSelected(position, shouldSetActionBarTitle());
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
    public static interface NavigationCallbacks {
        /**
         * Called when an item in the navigation is selected.
         */
        void onNavigationItemSelected(int position, boolean setTitle);
    }

    private static class ProfileViewHolder {
        public ImageView imageView;
        public TextView username, followers;
    }

    private class NavigationAdapter extends ArrayAdapter<NavItem> {

        public NavigationAdapter(Context context, int layoutResourceId, NavItem[] data) {
            super(context, layoutResourceId, data);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            NavItem navItem = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.nav_item, parent, false);
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
