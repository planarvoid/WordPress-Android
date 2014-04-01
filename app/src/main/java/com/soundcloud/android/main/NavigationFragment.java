package com.soundcloud.android.main;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;

import android.annotation.SuppressLint;
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

import javax.inject.Inject;
import java.util.EnumSet;

@SuppressLint("ValidFragment")
public class NavigationFragment extends Fragment {

    @VisibleForTesting
    static final String STATE_SELECTED_POSITION = "selected_navigation_position";
    private static final int NO_IMAGE = -1;

    @Inject
    ImageOperations mImageOperations;

    private NavigationCallbacks mCallbacks;

    private ListView mListView;

    private int mCurrentSelectedPosition = NavItem.STREAM.ordinal();

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

    public NavigationFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    protected NavigationFragment(ImageOperations imageOperations) {
        mImageOperations = imageOperations;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        configureLocalContextActionBar();
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
    }

    protected void configureLocalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);
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
            } else if (Actions.LIKES.equals(action)) {
                selectItem(NavItem.LIKES.ordinal());
                return true;
            } else if (Actions.EXPLORE.equals(action)) {
                selectItem(NavItem.EXPLORE.ordinal());
                return true;
            }
        }

        if (intent.getData() != null) {
            if (intent.getData().getLastPathSegment().equals("stream")) {
                selectItem(NavItem.STREAM.ordinal());
            } else if (intent.getData().getLastPathSegment().equals("explore")) {
                selectItem(NavItem.EXPLORE.ordinal());
            } else {
                // facebook deeplink, as they need to be routed through the launcher activity
                startActivity(new Intent(getActivity(), ResolveActivity.class).setAction(Intent.ACTION_VIEW)
                        .setData(intent.getData()));
                getActivity().finish();
            }
            return true;
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

    public void storeState(Bundle bundle) {
        bundle.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    public void initState(Bundle bundle) {
        if (bundle != null) {
            selectItem(bundle.getInt(STATE_SELECTED_POSITION));
        } else if (!handleIntent(getActivity().getIntent())) {
            selectItem(mCurrentSelectedPosition);
        }
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

        updateProfileItem(((SoundCloudApplication) getActivity().getApplication()).getLoggedInUser());

        return view;
    }

    public void updateProfileItem(User user) {
        mProfileViewHolder.username.setText(user.getUsername());
        int followersCount = user.followers_count < 0 ? 0 : user.followers_count;
        mProfileViewHolder.followers.setText(getResources().getQuantityString(
                R.plurals.number_of_followers, followersCount, followersCount));

        mImageOperations.displayWithPlaceholder(user.getUrn(),
                ImageSize.getFullImageSize(getResources()),
                mProfileViewHolder.imageView);
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
        getActivity().supportInvalidateOptionsMenu();
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
