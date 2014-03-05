package com.soundcloud.android.search;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.collections.EndlessPagingAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.PlaylistSummary;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class PlaylistResultsAdapter extends EndlessPagingAdapter<PlaylistSummary> {

    private final ImageOperations mImageOperations;

    @Inject
    public PlaylistResultsAdapter(ImageOperations imageOperations) {
        super(Consts.CARD_PAGE_SIZE);
        mImageOperations = imageOperations;
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        View itemView = View.inflate(parent.getContext(), R.layout.default_grid_item, null);
        ItemViewHolder viewHolder = new ItemViewHolder();
        viewHolder.imageView = (ImageView) itemView.findViewById(R.id.image);
        viewHolder.username = (TextView) itemView.findViewById(R.id.username);
        viewHolder.title = (TextView) itemView.findViewById(R.id.title);
        viewHolder.tagList = (TextView) itemView.findViewById(R.id.tag);
        viewHolder.trackCount = (TextView) itemView.findViewById(R.id.extra_info);
        itemView.setTag(viewHolder);

        return itemView;
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        final PlaylistSummary playlist = getItem(position);

        viewHolder.username.setText(playlist.getUsername());
        viewHolder.title.setText(playlist.getTitle());
        String tracksQuantity = itemView.getResources().getQuantityString(R.plurals.number_of_sounds,
                playlist.getTrackCount(), playlist.getTrackCount());
        viewHolder.trackCount.setText(tracksQuantity);
        viewHolder.tagList.setText(formatTags(playlist.getTags().subList(0, 2)));

        viewHolder.imageView.setBackgroundResource(R.drawable.placeholder_cells);
        final String artworkUri = playlist.getArtworkUrl(ImageSize.getFullImageSize(itemView.getResources()));
        mImageOperations.displayInGridView(artworkUri, viewHolder.imageView);
    }

    private String formatTags(List<String> tags) {
        return Joiner.on(", ").join(Lists.transform(tags, new Function<String, String>() {
            @Override
            public String apply(String tag) {
                return "#" + tag;
            }
        }));
    }

    @VisibleForTesting
    static class ItemViewHolder {
        public ImageView imageView;
        public TextView username, title, tagList, trackCount;
    }

}
