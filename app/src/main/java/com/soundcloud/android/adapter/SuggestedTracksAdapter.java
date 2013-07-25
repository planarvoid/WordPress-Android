package com.soundcloud.android.adapter;

import com.google.common.collect.Lists;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.model.SuggestedTrack;
import com.soundcloud.android.utils.images.ImageOptionsFactory;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class SuggestedTracksAdapter extends SpacedGridAdapter {

    public static final int INITIAL_LIST_SIZE = 20;

    private List<SuggestedTrack> mSuggestedTracks = Lists.newArrayListWithCapacity(INITIAL_LIST_SIZE);
    private DisplayImageOptions mDisplayImageOptions = ImageOptionsFactory.adapterView(R.drawable.placeholder_cells);

    public void addSuggestedTrack(SuggestedTrack suggestedTrack) {
        mSuggestedTracks.add(suggestedTrack);
    }

    @Override
    public int getCount() {
        return mSuggestedTracks.size();
    }

    @Override
    public SuggestedTrack getItem(int position) {
        return mSuggestedTracks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mSuggestedTracks.get(position).getId();
    }

    @Override
    protected View getGridItem(int position, View convertView, ViewGroup parent) {
        ItemViewHolder viewHolder;

        if (convertView == null){
            convertView = View.inflate(parent.getContext(), R.layout.suggested_track_grid_item, null);
            viewHolder = new ItemViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.suggested_track_image);
            viewHolder.username = (TextView) convertView.findViewById(R.id.username);
            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ItemViewHolder) convertView.getTag();
        }

        final SuggestedTrack suggestedTrack = getItem(position);
        viewHolder.username.setText(suggestedTrack.getUser().getUsername());
        viewHolder.title.setText(suggestedTrack.getTitle());
        ImageLoader.getInstance().displayImage(suggestedTrack.getArtworkUrl(), viewHolder.imageView, mDisplayImageOptions);
        return convertView;
    }

    protected int getNumColumns(Resources resources) {
        return resources.getInteger(R.integer.suggested_user_grid_num_columns);
    }

    @Override
    protected int getItemSpacingTopBottom(Resources resources) {
        return resources.getDimensionPixelSize(R.dimen.explore_suggested_track_item_spacing_outside_top_bottom);
    }

    @Override
    protected int getItemSpacingLeftRight(Resources resources) {
        return resources.getDimensionPixelSize(R.dimen.explore_suggested_track_item_spacing_outside_left_right);
    }

    private static class ItemViewHolder {
        public ImageView imageView;
        public TextView username, title;
    }
}
