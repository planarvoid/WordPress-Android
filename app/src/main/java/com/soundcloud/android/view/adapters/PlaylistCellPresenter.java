package com.soundcloud.android.view.adapters;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.PlaylistSummary;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class PlaylistCellPresenter implements CellPresenter<PlaylistSummary, View> {

    private final ImageOperations imageOperations;

    @Inject
    public PlaylistCellPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(int position, ViewGroup parent, int itemViewType) {
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
    public void bindItemView(int position, View itemView, List<PlaylistSummary> playlists) {
        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        final PlaylistSummary playlist = playlists.get(position);

        viewHolder.username.setText(playlist.getUsername());
        viewHolder.title.setText(playlist.getTitle());
        String tracksQuantity = itemView.getResources().getQuantityString(R.plurals.number_of_sounds,
                playlist.getTrackCount(), playlist.getTrackCount());
        viewHolder.trackCount.setText(tracksQuantity);
        viewHolder.tagList.setText(formatTags(playlist.getTags()));

        final ImageSize imageSize = ImageSize.getFullImageSize(itemView.getResources());
        imageOperations.displayInAdapterView(playlist.getUrn(), imageSize, viewHolder.imageView);
    }

    private String formatTags(List<String> tags) {
        if (tags.size() >= 2) {
            return Joiner.on(", ").join(Lists.transform(tags.subList(0, 2), new Function<String, String>() {
                @Override
                public String apply(String tag) {
                    return "#" + tag;
                }
            }));
        } else if (tags.size() == 1) {
            return "#" + tags.get(0);
        } else {
            return "";
        }
    }

    @VisibleForTesting
    static class ItemViewHolder {
        public ImageView imageView;
        public TextView username, title, tagList, trackCount;
    }

}
