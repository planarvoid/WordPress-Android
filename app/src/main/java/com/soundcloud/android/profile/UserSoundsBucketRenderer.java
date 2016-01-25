package com.soundcloud.android.profile;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class UserSoundsBucketRenderer implements CellRenderer<UserSoundsBucket> {
    private final TrackItemRenderer trackItemRenderer;
    private final PlaylistItemRenderer playlistItemRenderer;
    private final ImageOperations imageOperations;
    private final Resources resources;

    private TextView heading;
    private ViewGroup holder;
    private TextView viewAll;
    private LayoutInflater layoutInflater;

    @Inject
    UserSoundsBucketRenderer(
            TrackItemRenderer trackItemRenderer,
            PlaylistItemRenderer playlistItemRenderer,
            ImageOperations imageOperations,
            Resources resources) {
        this.trackItemRenderer = trackItemRenderer;
        this.playlistItemRenderer = playlistItemRenderer;
        this.imageOperations = imageOperations;
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
        bindListItems(bucket.getPagedRemoteCollection());
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

    private void bindListItems(PagedRemoteCollection pagedRemoteCollection) {
        clearListItems();

        for (PropertySet item : pagedRemoteCollection) {
            createAndBindPlayableView(PlayableItem.from(item));
        }
    }

    // Temporary for now! Simply renders the title of the playable.
    // In the next PR, this will actually render the correct items.
    private void createAndBindPlayableView(PlayableItem playableItem) {
        final TextView textView = new TextView(holder.getContext());
        textView.setText(playableItem.getTitle());
        holder.addView(textView);
    }

    private void clearListItems() {
        holder.removeAllViews();
    }
}
