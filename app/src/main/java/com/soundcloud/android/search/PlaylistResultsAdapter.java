package com.soundcloud.android.search;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.soundcloud.android.R;
import com.soundcloud.android.collections.EndlessPagingAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlaylistSummary;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

class PlaylistResultsAdapter extends EndlessPagingAdapter<PlaylistSummary> {

    private static final int PAGE_SIZE = 20;

    private final ImageOperations mImageOperations;

    @Inject
    public PlaylistResultsAdapter(ImageOperations imageOperations) {
        super(PAGE_SIZE);
        mImageOperations = imageOperations;
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        View itemView = View.inflate(parent.getContext(), R.layout.suggested_tracks_grid_item, null);
        ItemViewHolder viewHolder = new ItemViewHolder();
        viewHolder.imageView = (ImageView) itemView.findViewById(R.id.suggested_track_image);
        viewHolder.username = (TextView) itemView.findViewById(R.id.username);
        viewHolder.title = (TextView) itemView.findViewById(R.id.title);
        viewHolder.tagList = (TextView) itemView.findViewById(R.id.genre);
        viewHolder.trackCount = (TextView) itemView.findViewById(R.id.playcount);
        itemView.setTag(viewHolder);

        return itemView;
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        final PlaylistSummary playlist = getItem(position);

        viewHolder.username.setText(playlist.getUsername());
        viewHolder.title.setText(playlist.getTitle());
        viewHolder.trackCount.setText(String.valueOf(playlist.getTrackCount()));
        viewHolder.tagList.setText(Joiner.on(", ").join(playlist.getTags()));
    }

    @VisibleForTesting
    static class ItemViewHolder {
        public ImageView imageView;
        public TextView username, title, tagList, trackCount;
    }

}
