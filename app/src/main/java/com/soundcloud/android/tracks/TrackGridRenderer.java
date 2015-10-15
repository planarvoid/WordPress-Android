package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.util.CondensedNumberFormatter;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

//Delete this class when Explore is gone
@Deprecated
public class TrackGridRenderer implements CellRenderer<TrackItem> {

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;

    @Inject
    TrackGridRenderer(ImageOperations imageOperations, CondensedNumberFormatter numberFormatter) {
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        View itemView = View.inflate(parent.getContext(), R.layout.explore_grid_item, null);
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
    public void bindItemView(int position, View itemView, List<TrackItem> tracks) {
        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        TrackItem track = tracks.get(position);

        viewHolder.username.setText(track.getCreatorName());
        viewHolder.title.setText(track.getTitle());

        if (TextUtils.isEmpty(track.getGenre())) {
            viewHolder.genre.setVisibility(View.GONE);
        } else {
            viewHolder.genre.setText("#" + track.getGenre());
            viewHolder.genre.setVisibility(View.VISIBLE);
        }
        viewHolder.playcount.setText(numberFormatter.format(track.getPlayCount()));

        final ApiImageSize apiImageSize = ApiImageSize.getFullImageSize(itemView.getResources());
        imageOperations.displayInAdapterView(track.getEntityUrn(), apiImageSize, viewHolder.imageView);
    }

    @VisibleForTesting
    static class ItemViewHolder {
        public ImageView imageView;
        public TextView username, title, genre, playcount;
    }
}
