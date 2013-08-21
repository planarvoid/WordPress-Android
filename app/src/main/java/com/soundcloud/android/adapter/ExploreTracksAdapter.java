package com.soundcloud.android.adapter;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.images.ImageOptionsFactory;
import com.soundcloud.android.view.adapter.GridSpacer;
import rx.Observable;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ExploreTracksAdapter extends EndlessPagingAdapter<Track> {

    public static final int INITIAL_LIST_SIZE = 20;

    private DisplayImageOptions mDisplayImageOptions = ImageOptionsFactory.adapterView(R.drawable.placeholder_cells);
    private GridSpacer mGridSpacer;

    public ExploreTracksAdapter(Observable<Observable<Track>> pagingObservable) {
        super(pagingObservable, INITIAL_LIST_SIZE);
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
        getGridSpacer(itemView.getResources()).configureItemPadding(itemView, position, getCount());
    }

    /**
     * Lazy Grid Spacer initialization as it needs resources for configuration
     */
    private GridSpacer getGridSpacer(Resources resources) {
        if (mGridSpacer == null) {
            mGridSpacer = new GridSpacer(
                    resources.getDimensionPixelSize(R.dimen.explore_suggested_track_item_spacing_outside_left_right),
                    resources.getDimensionPixelSize(R.dimen.explore_suggested_track_item_spacing_outside_top_bottom),
                    resources.getInteger(R.integer.suggested_user_grid_num_columns)
            );
        }
        return mGridSpacer;
    }

    @VisibleForTesting
    static class ItemViewHolder {
        public ImageView imageView;
        public TextView username, title;
    }
}
