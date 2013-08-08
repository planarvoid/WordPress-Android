package com.soundcloud.android.adapter;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.images.ImageOptionsFactory;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class TrackExploreAdapter extends EndlessPagingAdapter<Track> {

    public static final int INITIAL_LIST_SIZE = 20;

    private DisplayImageOptions mDisplayImageOptions = ImageOptionsFactory.adapterView(R.drawable.placeholder_cells);

    public TrackExploreAdapter() {
        super(INITIAL_LIST_SIZE, R.layout.list_loading_item);
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        View itemView = View.inflate(parent.getContext(), R.layout.suggested_tracks_grid_item, null);
        ItemViewHolder viewHolder = new ItemViewHolder();
        viewHolder.imageView = (ImageView) itemView.findViewById(R.id.suggested_track_image);
        viewHolder.username = (TextView) itemView.findViewById(R.id.username);
        viewHolder.title = (TextView) itemView.findViewById(R.id.title);
        itemView.setTag(viewHolder);

        return itemView;
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        final Track track = getItem(position);
        viewHolder.username.setText(track.getUserName());
        viewHolder.title.setText(track.getTitle());
        ImageLoader.getInstance().displayImage(track.getArtwork(), viewHolder.imageView, mDisplayImageOptions);
    }

    @VisibleForTesting
    static class ItemViewHolder {
        public ImageView imageView;
        public TextView username, title;
    }
}
