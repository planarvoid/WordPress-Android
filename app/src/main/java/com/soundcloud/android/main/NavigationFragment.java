package com.soundcloud.android.main;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
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

    @Inject ImageOperations imageOperations;
    @Inject AccountOperations accountOperations;

    private NavigationCallbacks callbacks;

    private ListView listView;

    private int currentSelectedPosition = NavItem.STREAM.ordinal();

    private ProfileViewHolder profileViewHolder;

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

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationCallbacks {

        void onSmoothSelectItem(int position, boolean setTitle);

        void onSelectItem(int position, boolean setTitle);
    }

    // normal rows (below profile)
    private static final EnumSet<NavItem> TEXT_NAV_ITEMS =
            EnumSet.of(NavItem.STREAM, NavItem.EXPLORE, NavItem.LIKES, NavItem.PLAYLISTS);

    public NavigationFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    protected NavigationFragment(ImageOperations imageOperations, AccountOperations accountOperations) {
        this.imageOperations = imageOperations;
        this.accountOperations = accountOperations;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        configureLocalContextActionBar();
        try {
            callbacks = (NavigationCallbacks) activity;
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
        return currentSelectedPosition;
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
        listView.setItemChecked(currentSelectedPosition, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        listView = setupListView(inflater, container);
        return listView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    public void storeState(Bundle bundle) {
        bundle.putInt(STATE_SELECTED_POSITION, currentSelectedPosition);
    }

    public void initState(Bundle bundle) {
        if (bundle != null) {
            selectItem(bundle.getInt(STATE_SELECTED_POSITION));
        } else if (!handleIntent(getActivity().getIntent())) {
            selectItem(currentSelectedPosition);
        }
    }

    private ListView setupListView(LayoutInflater inflater, ViewGroup container) {
        ListView listView = (ListView) inflater.inflate(R.layout.fragment_navigation_listview, container, false);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                smoothSelectItem(position);
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

        profileViewHolder = new ProfileViewHolder();
        profileViewHolder.imageView = (ImageView) view.findViewById(R.id.avatar);
        profileViewHolder.username = (TextView) view.findViewById(R.id.username);
        profileViewHolder.followers = (TextView) view.findViewById(R.id.followers_count);

        updateProfileItem(accountOperations.getLoggedInUser());

        return view;
    }

    public void updateProfileItem(PublicApiUser user) {
        profileViewHolder.username.setText(user.getUsername());
        int followersCount = user.followers_count < 0 ? 0 : user.followers_count;
        profileViewHolder.followers.setText(getResources().getQuantityString(
                R.plurals.number_of_followers, followersCount, followersCount));

        imageOperations.displayWithPlaceholder(user.getUrn(),
                ApiImageSize.getFullImageSize(getResources()),
                profileViewHolder.imageView);
    }

    protected void selectItem(int position) {
        adjustSelectionForProfile(position);

        if (callbacks != null) {
            callbacks.onSelectItem(position, shouldSetActionBarTitle());
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    protected void smoothSelectItem(int position) {
        adjustSelectionForProfile(position);

        if (callbacks != null) {
            callbacks.onSmoothSelectItem(position, shouldSetActionBarTitle());
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    private void adjustSelectionForProfile(int position) {
        // TODO: Since the user profile currently opens in a new activity, we must not adjust the current selection
        // index. Remove this workaround when the user browser has become a fragment too
        if (position != NavItem.PROFILE.ordinal()) {
            currentSelectedPosition = position;
        }
    }

    protected boolean shouldSetActionBarTitle() {
        return true;
    }

    protected ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
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
            ViewHolder holder;
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
