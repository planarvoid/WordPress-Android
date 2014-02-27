package com.soundcloud.android.explore;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.collections.EndlessPagingAdapter;
import com.soundcloud.android.collections.views.GridSpacer;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.image.ImageSize;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Locale;
public class ExploreTracksAdapter extends EndlessPagingAdapter<TrackSummary> {

    private final ImageOperations mImageOperations;
    private GridSpacer mGridSpacer;

    @Inject
    public ExploreTracksAdapter(GridSpacer gridSpacer, ImageOperations imageOperations) {
        super(Consts.CARD_PAGE_SIZE, R.layout.grid_loading_item);
        mGridSpacer = gridSpacer;
        mImageOperations = imageOperations;
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        View itemView = View.inflate(parent.getContext(), R.layout.suggested_tracks_grid_item, null);
        ItemViewHolder viewHolder = new ItemViewHolder();
        viewHolder.imageView = (ImageView) itemView.findViewById(R.id.suggested_track_image);
        viewHolder.username = (TextView) itemView.findViewById(R.id.username);
        viewHolder.title = (TextView) itemView.findViewById(R.id.title);
        viewHolder.genre = (TextView) itemView.findViewById(R.id.genre);
        viewHolder.playcount = (TextView) itemView.findViewById(R.id.playcount);
        itemView.setTag(viewHolder);

        return itemView;
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        final TrackSummary track = getItem(position);

        // TODO : figure out why we are null here
        if (viewHolder == null) throw new IllegalArgumentException("VIEWHOLDER IS NULL");
        if (track == null) throw new IllegalArgumentException("TRACK IS NULL");

        viewHolder.username.setText(track.getUserName());
        viewHolder.title.setText(track.getTitle());

        if (TextUtils.isEmpty(track.getGenre())){
            viewHolder.genre.setVisibility(View.GONE);
        } else {
            viewHolder.genre.setText(track.getGenre().toUpperCase(Locale.getDefault()));
            viewHolder.genre.setVisibility(View.VISIBLE);
        }
        final String playcountWithCommas = ScTextUtils.formatNumberWithCommas(track.getStats().getPlaybackCount());
        viewHolder.playcount.setText(itemView.getResources().getString(R.string.playcount, playcountWithCommas));

        viewHolder.imageView.setBackgroundResource(R.drawable.placeholder_cells);
        final String artworkUri = track.getArtworkOrAvatar(ImageSize.getFullImageSize(itemView.getResources()));
        mImageOperations.displayInGridView(artworkUri, viewHolder.imageView);

        mGridSpacer.configureItemPadding(itemView, position, getCount());
    }

    @VisibleForTesting
    static class ItemViewHolder {
        public ImageView imageView;
        public TextView username, title, genre, playcount;
    }
}
