package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

class InlinePlaylistTracksAdapter extends ItemAdapter<Track> implements EmptyViewAware {

    private static final int INITIAL_SIZE = 20;

    private final ImageOperations imageOperations;

    private int emptyViewStatus = EmptyView.Status.WAITING;

    InlinePlaylistTracksAdapter(ImageOperations imageOperations) {
        super(INITIAL_SIZE);
        this.imageOperations = imageOperations;
    }

    @Override
    public int getItemViewType(int position) {
        return items.isEmpty() ? IGNORE_ITEM_VIEW_TYPE : super.getItemViewType(position);
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        Context context = parent.getContext();
        if (getItemViewType(position) == IGNORE_ITEM_VIEW_TYPE) {
            EmptyView emptyView = new EmptyViewBuilder()
                    .withImage(R.drawable.empty_playlists)
                    .withMessageText(context.getString(R.string.empty_playlist_title))
                    .withSecondaryText(context.getString(R.string.empty_playlist_description))
                    .build(context);
            emptyView.setPadding(0, ViewUtils.dpToPx(context, 48), 0, ViewUtils.dpToPx(context, 48));
            return emptyView;
        } else {
            return new PlayableRow(context, imageOperations);
        }
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        if (getItemViewType(position) == IGNORE_ITEM_VIEW_TYPE) {
            ((EmptyView) itemView).setStatus(emptyViewStatus);
        } else {
            ((PlayableRow) itemView).display(position, items.get(position));
        }
    }

    protected boolean hasContentItems() {
        return !items.isEmpty();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getCount() {
        return Math.max(1, items.size()); // at least 1 for the empty view
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != IGNORE_ITEM_VIEW_TYPE;
    }

    public void setEmptyViewStatus(int emptyViewStatus){
        this.emptyViewStatus = emptyViewStatus;
        notifyDataSetChanged();
    }
}
