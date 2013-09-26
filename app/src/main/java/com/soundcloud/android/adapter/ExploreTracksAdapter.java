package com.soundcloud.android.adapter;

import com.google.common.annotations.VisibleForTesting;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.images.ImageOptionsFactory;
import com.soundcloud.android.utils.images.ImageSize;
import com.soundcloud.android.view.adapter.GridSpacer;
import rx.Observable;
import rx.Observer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

public class ExploreTracksAdapter extends EndlessPagingAdapter<Track> {

    public static final int INITIAL_LIST_SIZE = 20;

    private DisplayImageOptions mDisplayImageOptions = ImageOptionsFactory.adapterView(R.drawable.placeholder_cells);
    private GridSpacer mGridSpacer;

    public ExploreTracksAdapter(Observable<Observable<Track>> pagingObservable, Observer<Track> itemObserver) {
        super(pagingObservable, itemObserver, INITIAL_LIST_SIZE);
        mGridSpacer = new GridSpacer();
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
        final Track track = getItem(position);
        viewHolder.username.setText(track.getUserName());
        viewHolder.title.setText(track.getTitle().toUpperCase(Locale.getDefault()));
        viewHolder.genre.setText(track.genre.toUpperCase(Locale.getDefault()));
        viewHolder.playcount.setText(itemView.getResources().getString(R.string.playcount, track.playback_count));
        ImageLoader.getInstance().displayImage(ImageSize.formatUriForPlayer(itemView.getContext(), track.getArtwork()), viewHolder.imageView, mDisplayImageOptions);
        mGridSpacer.configureItemPadding(itemView, position, getCount());
    }

    @VisibleForTesting
    static class ItemViewHolder {
        public ImageView imageView;
        public TextView username, title, genre, playcount;
    }
}
