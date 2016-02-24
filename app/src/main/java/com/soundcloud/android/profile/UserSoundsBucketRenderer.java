package com.soundcloud.android.profile;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistCardRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.android.view.adapters.TrackCardRenderer;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class UserSoundsBucketRenderer implements CellRenderer<UserSoundsBucket> {
    private final TrackItemRenderer trackItemRenderer;
    private final PlaylistItemRenderer playlistItemRenderer;
    private final TrackCardRenderer trackCardRenderer;
    private final PlaylistCardRenderer playlistCardRenderer;
    private final Resources resources;

    private TextView heading;
    private ViewGroup holder;
    private TextView viewAll;

    @Inject
    UserSoundsBucketRenderer(
            TrackItemRenderer trackItemRenderer,
            PlaylistItemRenderer playlistItemRenderer,
            TrackCardRenderer trackCardRenderer,
            PlaylistCardRenderer playlistCardRenderer,
            Resources resources) {
        this.trackItemRenderer = trackItemRenderer;
        this.playlistItemRenderer = playlistItemRenderer;
        this.trackCardRenderer = trackCardRenderer;
        this.playlistCardRenderer = playlistCardRenderer;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.user_sounds_bucket, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsBucket> buckets) {
        heading = ButterKnife.findById(itemView, R.id.heading);
        holder = ButterKnife.findById(itemView, R.id.thumbnail_container);
        viewAll = ButterKnife.findById(itemView, R.id.view_all);

        final UserSoundsBucket bucket = buckets.get(position);

        bindTitle(bucket.getTitle());
        bindListItems(bucket.getPagedRemoteCollection(), bucket.getCollectionType());
        bindViewAll(bucket);
    }

    private void bindViewAll(UserSoundsBucket bucket) {
        final boolean showViewAll = bucket.getPagedRemoteCollection().nextPageLink().isPresent();

        viewAll.setVisibility(showViewAll ? View.VISIBLE : View.GONE);
        viewAll.setText(buildViewAllTitle(bucket.getCollectionType()));
    }

    private String buildViewAllTitle(int collectionType) {
        switch (collectionType) {
            case UserSoundsTypes.SPOTLIGHT:
                return Strings.EMPTY;
            case UserSoundsTypes.TRACKS:
                return resources.getString(R.string.user_profile_tracks_view_all);
            case UserSoundsTypes.RELEASES:
                return resources.getString(R.string.user_profile_releases_view_all);
            case UserSoundsTypes.PLAYLISTS:
                return resources.getString(R.string.user_profile_playlists_view_all);
            case UserSoundsTypes.REPOSTS:
                return resources.getString(R.string.user_profile_reposts_view_all);
            case UserSoundsTypes.LIKES:
                return resources.getString(R.string.user_profile_likes_view_all);
            default:
                throw new UnsupportedOperationException("This collection type does not exist");
        }
    }

    public void bindTitle(String title) {
        heading.setText(title);
    }

    private void bindListItems(PagedRemoteCollection pagedRemoteCollection, int collectionType) {
        clearListItems();

        for (PropertySet item : pagedRemoteCollection) {
            final boolean isTrack = item.get(EntityProperty.URN).isTrack();
            final boolean shouldRenderCard = shouldRenderCard(collectionType);

            if (isTrack) {
                createAndBindTrackView(TrackItem.from(item), shouldRenderCard);
            } else {
                createAndBindPlaylistView(PlaylistItem.from(item), shouldRenderCard);
            }
        }
    }

    private void createAndBindTrackView(TrackItem track, boolean shouldRenderCard) {
        if (shouldRenderCard) {
            createAndBindTrackCard(track);
        } else {
            createAndBindTrackItem(track);
        }
    }

    private void createAndBindTrackCard(TrackItem track) {
        final View itemView = trackCardRenderer.createItemView(holder);
        trackCardRenderer.bindTrackCard(track, itemView, 0);
        holder.addView(itemView);
        addCardDivider(itemView);
    }

    private void createAndBindPlaylistView(PlaylistItem playableItem, boolean shouldRenderCard) {
        if (shouldRenderCard) {
            createAndBindPlaylistCard(playableItem);
        } else {
            createAndBindPlaylistItem(playableItem);
        }
    }

    private void createAndBindPlaylistItem(PlaylistItem playlist) {
        final View itemView = playlistItemRenderer.createItemView(holder);
        playlistItemRenderer.bindPlaylistView(playlist, itemView);
        itemView.setBackground(itemView.getContext().getDrawable(R.drawable.divider_bottom));
        holder.addView(itemView);
    }

    private void createAndBindPlaylistCard(PlaylistItem playlist) {
        final View itemView = playlistCardRenderer.createItemView(holder);
        playlistCardRenderer.bindPlaylistCardView(playlist, itemView);
        holder.addView(itemView);
        addCardDivider(itemView);
    }

    private void addCardDivider(View itemView) {
        final Context context = itemView.getContext();
        final View divider = new View(context);
        divider.setBackground(context.getDrawable(R.color.playlist_background));
        divider.setMinimumHeight(context.getResources().getDimensionPixelOffset(R.dimen.profile_card_divider));
        holder.addView(divider);
    }

    private void createAndBindTrackItem(TrackItem track) {
        final View itemView = trackItemRenderer.createItemView(holder);
        trackItemRenderer.bindTrackView(track, itemView, 0);
        itemView.setBackground(itemView.getContext().getDrawable(R.drawable.divider_bottom));
        holder.addView(itemView);
    }

    private void clearListItems() {
        holder.removeAllViews();
    }

    private boolean shouldRenderCard(int collectionType) {
        switch (collectionType) {
            case UserSoundsTypes.SPOTLIGHT:
                return true;
            default:
                return false;
        }
    }
}
