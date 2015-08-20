package com.soundcloud.android.main;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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

    private static final int NO_TEXT = -1;
    private static final int NO_IMAGE = -1;

    private static final String STREAM = "stream";
    private static final String SOUNDCLOUD_COM = "soundcloud.com";

    @Inject ImageOperations imageOperations;
    @Inject AccountOperations accountOperations;
    @Inject FeatureOperations featureOperations;
    @Inject FeatureFlags featureFlags;
    @Inject EventBus eventBus;

    private NavigationCallbacks callbacks;
    private ListView listView;
    private View upsell;
    private ProfileViewHolder profileViewHolder;

    private NavItem currentSelectedItem = NavItem.STREAM;
    private NavigationAdapter adapter;

    public NavigationFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    protected NavigationFragment(ImageOperations imageOperations, AccountOperations accountOperations,
                                 FeatureOperations featureOperations, FeatureFlags featureFlags, EventBus eventBus) {
        this.imageOperations = imageOperations;
        this.accountOperations = accountOperations;
        this.featureOperations = featureOperations;
        this.featureFlags = featureFlags;
        this.eventBus = eventBus;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            callbacks = (NavigationCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationCallbacks.");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // on attach is a bit too quick the toolbar is not available yet?
        configureLocalContextActionBar();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public boolean handleIntent(Intent intent) {
        final String action = intent.getAction();
        if (ScTextUtils.isNotBlank(action)) {
            switch (action) {
                case Actions.STREAM:
                    selectItem(NavItem.STREAM);
                    return true;
                case Actions.LIKES:
                    selectItem(NavItem.LIKES);
                    return true;
                case Actions.EXPLORE:
                    selectItem(NavItem.EXPLORE);
                    return true;
            }
        }

        final Uri data = intent.getData();
        if (data != null) {
            if (shouldGoToStream(data)) {
                selectItem(NavItem.STREAM);
                return true;
            } else if (data.getLastPathSegment().equals("explore")) {
                selectItem(NavItem.EXPLORE);
                return true;
            } else if (ResolveActivity.accept(data, getResources())) {
                // facebook deeplink, as they need to be routed through the launcher activity
                startActivity(new Intent(getActivity(), ResolveActivity.class).setAction(Intent.ACTION_VIEW)
                        .setData(data));
                getActivity().finish();
                return true;
            }
        }
        return false;
    }

    public boolean handleBackPressed() {
        return false;
    }

    private boolean shouldGoToStream(Uri data) {
        final String host = data.getHost();
        return host != null && (STREAM.equals(host)
                        || STREAM.equals(data.getLastPathSegment())
                        || (host.contains(SOUNDCLOUD_COM) && ScTextUtils.isBlank(data.getPath())));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        setupListView(inflater, layout);
        upsell = layout.findViewById(R.id.nav_upsell);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update the checked state of the nav items to the last known position. It's important to do this in onResume
        // as long as the user profile opens in a new activity, since when returning via the up button would otherwise
        // not update it to the last selected content fragment
        listView.setItemChecked(toItemPositionInListView(currentSelectedItem), true);
        updateUpsellVisibility();
    }

    private int toItemPositionInListView(NavItem currentSelectedItem) {
        // The header occupies the first position in the list view but is absent from the adapter.
        return adapter.getPosition(currentSelectedItem) + 1;
    }

    private void updateUpsellVisibility() {
        if (featureOperations.upsellMidTier()) {
            eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forNavImpression());
            upsell.setVisibility(View.VISIBLE);
            upsell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectItem(NavItem.UPSELL);
                }
            });
        } else {
            upsell.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    public void storeState(Bundle bundle) {
        bundle.putInt(STATE_SELECTED_POSITION, currentSelectedItem.ordinal());
    }

    public void initState(Bundle bundle) {
        if (bundle != null) {
            selectItem(NavItem.values()[bundle.getInt(STATE_SELECTED_POSITION)]);
        } else if (!handleIntent(getActivity().getIntent())) {
            selectItem(currentSelectedItem);
        }
    }

    public void updateProfileItem(PropertySet user) {
        profileViewHolder.username.setText(user.get(UserProperty.USERNAME));
        imageOperations.displayWithPlaceholder(user.get(UserProperty.URN),
                ApiImageSize.getFullImageSize(getResources()),
                profileViewHolder.imageView);
        setFollowerCount(user);
    }

    private void setFollowerCount(PropertySet user) {
        final int followersCount = Math.max(0, user.get(UserProperty.FOLLOWERS_COUNT));
        profileViewHolder.followers.setText(getResources().getQuantityString(
                R.plurals.number_of_followers, followersCount, followersCount));
    }

    protected void configureLocalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);
    }

    protected void selectItem(NavItem item) {
        adjustSelectionIfNecessary(item);

        if (callbacks != null) {
            callbacks.onSelectItem(item);
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    protected void smoothSelectItem(int position, NavItem item) {
        adjustSelectionIfNecessary(item);

        if (callbacks != null) {
            callbacks.onSmoothSelectItem(item);
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    protected ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    private ListView setupListView(LayoutInflater inflater, ViewGroup layout) {
        this.listView = (ListView) layout.findViewById(R.id.nav_list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                smoothSelectItem(position, navItemAtPosition(position));
            }
        });
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        View userProfileHeader = setupUserProfileHeader(inflater, listView);
        listView.addHeaderView(userProfileHeader);

        adapter = new NavigationAdapter(getActivity(), R.layout.nav_item, getEnabledNavItems());
        listView.setAdapter(adapter);

        return listView;
    }

    private NavItem navItemAtPosition(int position) {
        if (position == 0) {
            return NavItem.PROFILE;
        } else {
            return (NavItem) listView.getAdapter().getItem(position);
        }
    }

    private View setupUserProfileHeader(LayoutInflater inflater, ListView listView) {
        final View view = inflater.inflate(R.layout.nav_profile_item, listView, false);

        profileViewHolder = new ProfileViewHolder();
        profileViewHolder.imageView = (ImageView) view.findViewById(R.id.avatar);
        profileViewHolder.username = (TextView) view.findViewById(R.id.username);
        profileViewHolder.followers = (TextView) view.findViewById(R.id.followers_count);

        if (accountOperations.isUserLoggedIn()) {
            updateProfileItem(accountOperations.getLoggedInUser().toPropertySet());
        }

        return view;
    }

    private void adjustSelectionIfNecessary(NavItem item) {
        if (NavItem.isSelectable(item)) {
            currentSelectedItem = item;
        }
    }

    @VisibleForTesting
    NavItem getCurrentSelectedItem() {
        return currentSelectedItem;
    }

    public enum NavItem {
        PROFILE(NO_TEXT, NO_IMAGE),
        STATIONS(R.string.side_menu_stations, NO_IMAGE),
        STREAM(R.string.side_menu_stream, R.drawable.nav_stream_states),
        EXPLORE(R.string.side_menu_explore, R.drawable.nav_explore_states),
        LIKES(R.string.side_menu_likes, R.drawable.nav_likes_states),
        PLAYLISTS(R.string.side_menu_playlists, R.drawable.nav_playlists_states),
        UPSELL(NO_TEXT, NO_IMAGE),
        NONE(NO_TEXT, NO_IMAGE);

        private static final EnumSet<NavItem> SELECTABLE = EnumSet.of(STATIONS, STREAM, EXPLORE, LIKES, PLAYLISTS);

        private final int textId;
        private final int imageId;

        NavItem(int textId, int imageId) {
            this.textId = textId;
            this.imageId = imageId;
        }

        public static boolean isSelectable(NavItem item) {
            return SELECTABLE.contains(item);
        }
    }

    public NavItem[] getEnabledNavItems() {
        if (featureFlags.isEnabled(Flag.STATIONS)) {
            return new NavItem[]{NavItem.STATIONS, NavItem.STREAM, NavItem.EXPLORE, NavItem.LIKES, NavItem.PLAYLISTS};
        } else {
            return new NavItem[]{NavItem.STREAM, NavItem.EXPLORE, NavItem.LIKES, NavItem.PLAYLISTS};
        }
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public interface NavigationCallbacks {

        void onSmoothSelectItem(NavItem item);

        void onSelectItem(NavItem item);
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

            final View view;
            if (convertView == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.nav_item, parent, false);
                holder = new ViewHolder();
                holder.icon = (ImageView) view.findViewById(R.id.nav_item_image);
                holder.text = (TextView) view.findViewById(R.id.nav_item_text);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }
            holder.text.setText(navItem.textId);
            holder.icon.setImageResource(navItem.imageId);
            return view;
        }

        private class ViewHolder {
            ImageView icon;
            TextView text;
        }

    }
}
