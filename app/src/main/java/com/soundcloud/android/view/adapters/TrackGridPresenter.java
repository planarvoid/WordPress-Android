package com.soundcloud.android.view.adapters;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
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
import java.util.List;

public class TrackGridPresenter implements CellPresenter<TrackSummary, View> {

    private final ImageOperations imageOperations;

    @Inject
    TrackGridPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(int position, ViewGroup parent, int itemViewType) {
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
    public void bindItemView(int position, View itemView, List<TrackSummary> tracks) {
        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        TrackSummary track = tracks.get(position);

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
