package com.soundcloud.android.suggestedcreators;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;

import android.view.View;

import java.util.Map;

class SuggestedCreatorsAdapter extends RecyclerItemAdapter<SuggestedCreatorItem, RecyclerItemAdapter.ViewHolder> {
    private static final int SUGGESTED_CREATORS_TYPE = 0;

    SuggestedCreatorsAdapter(SuggestedCreatorRenderer suggestedCreatorRenderer) {
        super(suggestedCreatorRenderer);
    }

    void onFollowingEntityChange(EntityStateChangedEvent event) {
        final Map<Urn, PropertySet> changeMap = event.getChangeMap();
        for (Urn urn : changeMap.keySet()) {
            final Boolean followed = changeMap.get(urn).get(UserProperty.IS_FOLLOWED_BY_ME);
            setFollowingState(urn, followed);
        }
    }

    private void setFollowingState(Urn urn, Boolean isFollowing) {
        for (SuggestedCreatorItem item : items) {
            if (item.user().urn().equals(urn) && item.following != isFollowing) {
                item.following = isFollowing;
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
