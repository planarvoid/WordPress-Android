package com.soundcloud.android.suggestedcreators;

import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

class SuggestedCreatorsAdapter extends RecyclerItemAdapter<SuggestedCreatorItem, RecyclerView.ViewHolder> {
    private static final int SUGGESTED_CREATORS_TYPE = 0;
    private final SuggestedCreatorRenderer suggestedCreatorRenderer;

    SuggestedCreatorsAdapter(SuggestedCreatorRenderer suggestedCreatorRenderer) {
        super(suggestedCreatorRenderer);
        this.suggestedCreatorRenderer = suggestedCreatorRenderer;
    }

    void unsubscribe() {
        suggestedCreatorRenderer.unsubscribe();
    }

    void onFollowingEntityChange(FollowingStatusEvent event) {
        for (SuggestedCreatorItem item : items) {
            if (event.urn().equals(item.creator().urn()) && item.following != event.isFollowed()) {
                item.following = event.isFollowed();
                notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return SUGGESTED_CREATORS_TYPE;
    }
}
