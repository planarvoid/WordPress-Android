package com.soundcloud.android.explore;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.collections.EndlessPagingAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.utils.ScTextUtils;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class ExploreTracksAdapter extends EndlessPagingAdapter<TrackSummary> {

    private final ImageOperations imageOperations;

    @Inject
    public ExploreTracksAdapter(ImageOperations imageOperations) {
        super(Consts.CARD_PAGE_SIZE, R.layout.grid_loading_item);
        this.imageOperations = imageOperations;
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        View itemView = View.inflate(parent.getContext(), R.layout.default_grid_item, null);
        ItemViewHolder viewHolder = new ItemViewHolder();
        viewHolder.imageView = (ImageView) itemView.findViewById(R.id.image);
        viewHolder.username = (TextView) itemView.findViewById(R.id.username);
        viewHolder.title = (TextView) itemView.findViewById(R.id.title);
        viewHolder.genre = (TextView) itemView.findViewById(R.id.tag);
        viewHolder.playcount = (TextView) itemView.findViewById(R.id.extra_info);
        itemView.setTag(viewHolder);

        viewHolder.playcount.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stats_plays, 0, 0, 0);
        return itemView;
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        final TrackSummary track = getItem(position);

        viewHolder.username.setText(track.getUserName());
        viewHolder.title.setText(track.getTitle());

        if (TextUtils.isEmpty(track.getGenre())) {
            viewHolder.genre.setVisibility(View.GONE);
        } else {
            viewHolder.genre.setText("#" + track.getGenre());
            viewHolder.genre.setVisibility(View.VISIBLE);
        }
        final String playcountWithCommas = ScTextUtils.formatNumberWithCommas(track.getStats().getPlaybackCount());
        viewHolder.playcount.setText(playcountWithCommas);

        final ImageSize imageSize = ImageSize.getFullImageSize(itemView.getResources());
        imageOperations.displayInAdapterView(track.getUrn(), imageSize, viewHolder.imageView);
    }

    @VisibleForTesting
    static class ItemViewHolder {
        public ImageView imageView;
        public TextView username, title, genre, playcount;
    }
}
