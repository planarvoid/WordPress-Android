package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.android.view.adapters.UserItemRenderer;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

class SearchPremiumContentRenderer implements CellRenderer<SearchPremiumItem> {

    interface OnPremiumContentClickListener {
        void onPremiumContentHelpClicked(Context context);
        void onPremiumItemClicked(View view, List<ListItem> premiumItems);
        void onPremiumContentViewAllClicked(Context context, List<PropertySet> premiumItemsSource, Optional<Link> nextHref);
    }

    private final TrackItemRenderer trackItemRenderer;
    private final PlaylistItemRenderer playlistItemRenderer;
    private final UserItemRenderer userItemRenderer;
    private final Resources resources;
    private final FeatureOperations featureOperations;

    private OnPremiumContentClickListener premiumContentListener;

    private View trackItemView;
    private View playListItemView;
    private View userItemView;

    @Inject
    SearchPremiumContentRenderer(TrackItemRenderer trackItemRenderer, PlaylistItemRenderer playlistItemRenderer,
                                 UserItemRenderer userItemRenderer, Resources resources, FeatureOperations featureOperations) {
        this.trackItemRenderer = trackItemRenderer;
        this.playlistItemRenderer = playlistItemRenderer;
        this.userItemRenderer = userItemRenderer;
        this.resources = resources;
        this.featureOperations = featureOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View premiumItemView = inflater.inflate(R.layout.search_premium_item, parent, false);
        addItemView(premiumItemView);
        return premiumItemView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchPremiumItem> premiumItems) {
        final SearchPremiumItem premiumItem = premiumItems.get(0);
        final PropertySet premiumItemSource = premiumItem.getSourceSet().get(0);
        final SearchResultItem item = SearchResultItem.fromUrn(premiumItemSource.get(EntityProperty.URN));

        if (item.isTrack()) {
            trackItemView.setVisibility(View.VISIBLE);
            trackItemView.setOnClickListener(new ListItemClickListener(premiumContentListener, premiumItems));
            trackItemRenderer.bindItemView(position, trackItemView,
                    Collections.singletonList(TrackItem.from(premiumItemSource)));
        } else if (item.isPlaylist()) {
            playListItemView.setVisibility(View.VISIBLE);
            playListItemView.setOnClickListener(new ListItemClickListener(premiumContentListener, premiumItems));
            playlistItemRenderer.bindItemView(position, playListItemView,
                    Collections.singletonList(PlaylistItem.from(premiumItemSource)));
        } else if (item.isUser()) {
            userItemView.setVisibility(View.VISIBLE);
            userItemView.setOnClickListener(new ListItemClickListener(premiumContentListener, premiumItems));
            userItemRenderer.bindItemView(position, userItemView,
                    Collections.singletonList(UserItem.from(premiumItemSource)));
        }

        getView(itemView, R.id.premium_item_container).setOnClickListener(null);

        final TextView resultsCountTextView = getTextView(itemView, R.id.results_count);
        resultsCountTextView.setText(getResultsCountText(premiumItem));
        resultsCountTextView.setOnClickListener(null);
        getTextView(itemView, R.id.view_all).setText(getViewAllText());

        setupViewAllButton(itemView, premiumItems);
        setupHighTierHelpItem(itemView);
    }

    private void setupViewAllButton(View itemView, List<SearchPremiumItem> premiumItems) {
        final View viewAllResultsView = getView(itemView, R.id.view_all_container);
        final View viewAllResultsDividerView  = getView(itemView, R.id.view_all_container_divider);

        final SearchPremiumItem premiumItem = premiumItems.get(0);
        if (premiumItem.getResultsCount() > 1) {
            viewAllResultsDividerView.setVisibility(View.VISIBLE);
            viewAllResultsView.setVisibility(View.VISIBLE);
            viewAllResultsView.setOnClickListener(new ViewAllClickListener(premiumContentListener, premiumItems));
        } else {
            viewAllResultsDividerView.setVisibility(View.GONE);
            viewAllResultsView.setVisibility(View.GONE);
        }
    }

    private void addItemView(View premiumItemView) {
        final ViewGroup itemContainer = (ViewGroup) premiumItemView.findViewById(R.id.premium_item_container);
        final int positionIndex = itemContainer.indexOfChild(premiumItemView.findViewById(R.id.results_count_section));

        trackItemView = trackItemRenderer.createItemView(itemContainer);
        playListItemView = playlistItemRenderer.createItemView(itemContainer);
        userItemView = userItemRenderer.createItemView(itemContainer);

        itemContainer.addView(trackItemView, positionIndex + 1);
        itemContainer.addView(playListItemView, positionIndex + 2);
        itemContainer.addView(userItemView, positionIndex + 3);

        setListItemBackground(premiumItemView, trackItemView);
        setListItemBackground(premiumItemView, playListItemView);
        setListItemBackground(premiumItemView, userItemView);

        trackItemView.setVisibility(View.GONE);
        playListItemView.setVisibility(View.GONE);
        userItemView.setVisibility(View.GONE);
    }

    private void setupHighTierHelpItem(View itemView) {
        final View helpItemView = getView(itemView, R.id.help);
        if (featureOperations.upsellHighTier()) {
            helpItemView.setVisibility(View.VISIBLE);
            helpItemView.setOnClickListener(new HelpClickListener(premiumContentListener));
        } else {
            helpItemView.setVisibility(View.GONE);
        }
    }

    private void setListItemBackground(View containerView, View listItemView) {
        TypedValue outValue = new TypedValue();
        containerView.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        listItemView.setBackgroundResource(outValue.resourceId);
    }

    private TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }

    private View getView(final View convertView, final int id) {
        return convertView.findViewById(id);
    }

    private String getResultsCountText(SearchPremiumItem premiumItem) {
        return resources.getString(R.string.search_premium_content_results_count, premiumItem.getResultsCount());
    }

    private String getViewAllText() {
        return resources.getString(R.string.search_premium_content_view_all).toUpperCase(Locale.getDefault());
    }

    void setPremiumContentListener(OnPremiumContentClickListener listener) {
        Preconditions.checkArgument(listener != null, "Click listener must not be null");
        this.premiumContentListener = listener;
    }

    private static class ListItemClickListener implements View.OnClickListener {
        private final OnPremiumContentClickListener listener;
        private final List<SearchPremiumItem> premiumItems;

        private ListItemClickListener(OnPremiumContentClickListener listener, List<SearchPremiumItem> premiumItems) {
            this.listener = listener;
            this.premiumItems = premiumItems;
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                final List<PropertySet> propertySets = premiumItems.get(0).getSourceSet();
                final List<ListItem> premiumItemList = new ArrayList<>(propertySets.size());
                for (PropertySet source : propertySets) {
                    premiumItemList.add(SearchResultItem.fromPropertySet(source).build());
                }
                listener.onPremiumItemClicked(view, premiumItemList);
            }
        }
    }

    private static class ViewAllClickListener implements View.OnClickListener {
        private final OnPremiumContentClickListener listener;
        private final List<SearchPremiumItem> premiumItems;

        private ViewAllClickListener(OnPremiumContentClickListener listener, List<SearchPremiumItem> premiumItems) {
            this.listener = listener;
            this.premiumItems = premiumItems;
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                final SearchPremiumItem searchPremiumItem = premiumItems.get(0);
                listener.onPremiumContentViewAllClicked(view.getContext(),
                        searchPremiumItem.getSourceSet(), searchPremiumItem.getNextHref() );
            }
        }
    }

    private static class HelpClickListener implements View.OnClickListener {
        private final OnPremiumContentClickListener listener;

        private HelpClickListener(OnPremiumContentClickListener listener) {
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                listener.onPremiumContentHelpClicked(view.getContext());
            }
        }
    }
}
